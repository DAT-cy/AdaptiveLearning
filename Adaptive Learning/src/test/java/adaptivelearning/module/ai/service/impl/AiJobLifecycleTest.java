package adaptivelearning.module.ai.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import adaptivelearning.config.response.CommonException;
import adaptivelearning.config.response.ErrorCode;
import adaptivelearning.module.ai.dto.request.AiGradeRequest;
import adaptivelearning.module.ai.entity.AiEvaluation;
import adaptivelearning.module.ai.entity.AiJob;
import adaptivelearning.module.ai.gateway.AiProvider;
import adaptivelearning.module.ai.repository.AiEvaluationRepository;
import adaptivelearning.module.ai.repository.AiJobRepository;
import adaptivelearning.module.ai.repository.RubricRepository;
import adaptivelearning.module.ai.service.impl.AiServiceImpl;
import adaptivelearning.module.attempts.entity.ExamAttempt;
import adaptivelearning.module.attempts.repository.ExamAttemptRepository;
import adaptivelearning.module.exams.entity.ExamQuestion;
import adaptivelearning.module.exams.repository.ExamQuestionRepository;
import adaptivelearning.module.exams.repository.ExamRepository;
import adaptivelearning.shared.enums.AiJobStatus;
import adaptivelearning.shared.enums.AiJobType;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AiJobLifecycleTest {

    private AiEvaluationRepository evaluations;
    private AiJobRepository jobs;
    private AiProvider provider;
    private ExamAttemptRepository attempts;
    private ExamQuestionRepository examQuestions;
    private AiServiceImpl service;

    @BeforeEach
    void setUp() {
        evaluations = mock(AiEvaluationRepository.class);
        jobs = mock(AiJobRepository.class);
        provider = mock(AiProvider.class);
        attempts = mock(ExamAttemptRepository.class);
        examQuestions = mock(ExamQuestionRepository.class);
        service = new AiServiceImpl(evaluations, jobs, mock(RubricRepository.class), provider,
                new ObjectMapper(), attempts, examQuestions, mock(ExamRepository.class));
        when(attempts.findById(50L)).thenReturn(Optional.of(
                ExamAttempt.builder().attemptId(50L).userId(5L).examId(20L).build()));
        when(examQuestions.findByExamIdAndQuestionId(20L, 7L)).thenReturn(Optional.of(
                ExamQuestion.builder().examId(20L).questionId(7L).build()));
        when(evaluations.findByAttemptIdAndQuestionId(50L, 7L)).thenReturn(Optional.empty());
        when(evaluations.save(any(AiEvaluation.class))).thenAnswer(invocation -> {
            AiEvaluation evaluation = invocation.getArgument(0);
            if (evaluation.getEvaluationId() == null) evaluation.setEvaluationId(90L);
            return evaluation;
        });
        when(jobs.save(any(AiJob.class))).thenAnswer(invocation -> {
            AiJob job = invocation.getArgument(0);
            if (job.getJobId() == null) job.setJobId(80L);
            return job;
        });
    }

    @Test
    void submitEnqueuesPendingJobAndDoesNotCallProviderInline() {
        when(jobs.findByIdempotencyKey("GRADE_WRITING:50:7")).thenReturn(Optional.empty());

        var response = service.submitGradeJob(request(), 5L, false);

        assertEquals(AiJobStatus.PENDING, response.getStatus());
        verify(jobs).save(any(AiJob.class));
        verify(evaluations).save(any(AiEvaluation.class));
        verify(provider, never()).chatCompletionSync(anyString(), anyString(), anyString(), any(Integer.class));
    }

    @Test
    void duplicateSubmitReusesExistingJobAndEvaluation() {
        AiJob existing = job(80L, AiJobStatus.SUCCEEDED, 0);
        AiEvaluation evaluation = evaluation(AiJobStatus.SUCCEEDED);
        when(jobs.findByIdempotencyKey("GRADE_WRITING:50:7")).thenReturn(Optional.of(existing));
        when(evaluations.findByAttemptIdAndQuestionId(50L, 7L)).thenReturn(Optional.of(evaluation));

        var response = service.submitGradeJob(request(), 5L, false);

        assertEquals(80L, response.getJobId());
        assertEquals(AiJobStatus.SUCCEEDED, response.getStatus());
        verify(jobs, never()).save(any(AiJob.class));
        verify(evaluations, never()).save(any(AiEvaluation.class));
        verify(provider, never()).chatCompletionSync(anyString(), anyString(), anyString(), any(Integer.class));
    }

    @Test
    void pollClaimsPendingJobAndPersistsSuccessfulResultReference() {
        AiJob candidate = job(80L, AiJobStatus.PENDING, 0);
        AiJob claimed = job(80L, AiJobStatus.PROCESSING, 0);
        AiEvaluation pending = evaluation(AiJobStatus.PENDING);
        when(jobs.findByStatusInOrderByJobIdAsc(anyList())).thenReturn(List.of(candidate));
        when(jobs.claimIfStatusMatches(any(), anyList(), any(), any())).thenReturn(1);
        when(jobs.findById(80L)).thenReturn(Optional.of(claimed));
        when(evaluations.findByAttemptIdAndQuestionIdAndStatusIn(50L, 7L,
                List.of(AiJobStatus.PENDING, AiJobStatus.RETRYING))).thenReturn(Optional.of(pending));
        when(provider.chatCompletionSync(anyString(), anyString(), anyString(), any(Integer.class)))
                .thenReturn(validResponse());
        when(evaluations.findByAttemptIdAndQuestionId(50L, 7L)).thenReturn(Optional.of(pending));

        service.processPendingJobs();

        assertEquals(AiJobStatus.SUCCEEDED, claimed.getStatus());
        assertEquals("AI_EVALUATION", claimed.getOutputRefType());
        assertEquals(90L, claimed.getOutputRefId());
        assertEquals(AiJobStatus.SUCCEEDED, pending.getStatus());
        verify(provider).chatCompletionSync(anyString(), anyString(), anyString(), any(Integer.class));
    }

    @Test
    void claimCompareAndSetPreventsDuplicateWorkersFromProcessingSameJob() {
        AiJob candidate = job(80L, AiJobStatus.PENDING, 0);
        when(jobs.findByStatusInOrderByJobIdAsc(anyList())).thenReturn(List.of(candidate));
        when(jobs.claimIfStatusMatches(any(), anyList(), any(), any())).thenReturn(0);

        service.processPendingJobs();

        verify(jobs, never()).findById(80L);
        verify(provider, never()).chatCompletionSync(anyString(), anyString(), anyString(), any(Integer.class));
    }

    @Test
    void providerFailureMovesJobAndEvaluationToRetryingAndStoresNoProviderBody() {
        AiJob candidate = job(80L, AiJobStatus.PENDING, 0);
        AiJob claimed = job(80L, AiJobStatus.PROCESSING, 0);
        AiEvaluation pending = evaluation(AiJobStatus.PENDING);
        when(jobs.findByStatusInOrderByJobIdAsc(anyList())).thenReturn(List.of(candidate));
        when(jobs.claimIfStatusMatches(any(), anyList(), any(), any())).thenReturn(1);
        when(jobs.findById(80L)).thenReturn(Optional.of(claimed));
        when(evaluations.findByAttemptIdAndQuestionIdAndStatusIn(eq(50L), eq(7L), anyList()))
                .thenReturn(Optional.of(pending));
        when(evaluations.findByAttemptIdAndQuestionId(50L, 7L)).thenReturn(Optional.of(pending));
        when(provider.chatCompletionSync(anyString(), anyString(), anyString(), any(Integer.class)))
                .thenThrow(new IllegalStateException("sensitive upstream details"));

        service.processPendingJobs();

        assertEquals(AiJobStatus.RETRYING, claimed.getStatus());
        assertEquals(1, claimed.getRetryCount());
        assertEquals(AiJobStatus.RETRYING, pending.getStatus());
        org.junit.jupiter.api.Assertions.assertFalse(claimed.getErrorMessage().contains("sensitive upstream details"));
    }

    @Test
    void providerFailureAtRetryLimitMovesJobAndEvaluationToFailed() {
        AiJob candidate = job(80L, AiJobStatus.RETRYING, 2);
        AiJob claimed = job(80L, AiJobStatus.PROCESSING, 2);
        AiEvaluation evaluation = evaluation(AiJobStatus.RETRYING);
        when(jobs.findByStatusInOrderByJobIdAsc(anyList())).thenReturn(List.of(candidate));
        when(jobs.claimIfStatusMatches(any(), anyList(), any(), any())).thenReturn(1);
        when(jobs.findById(80L)).thenReturn(Optional.of(claimed));
        when(evaluations.findByAttemptIdAndQuestionIdAndStatusIn(eq(50L), eq(7L), anyList()))
                .thenReturn(Optional.of(evaluation));
        when(evaluations.findByAttemptIdAndQuestionId(50L, 7L)).thenReturn(Optional.of(evaluation));
        when(provider.chatCompletionSync(anyString(), anyString(), anyString(), any(Integer.class)))
                .thenThrow(new IllegalStateException("upstream rejected"));

        service.processPendingJobs();

        assertEquals(AiJobStatus.FAILED, claimed.getStatus());
        assertEquals(3, claimed.getRetryCount());
        assertEquals(AiJobStatus.FAILED, evaluation.getStatus());
    }

    @Test
    void retryEndpointOnlyResetsFailedJobAndItsEvaluation() {
        AiJob failed = job(80L, AiJobStatus.FAILED, 3);
        AiEvaluation failedEvaluation = evaluation(AiJobStatus.FAILED);
        when(jobs.findById(80L)).thenReturn(Optional.of(failed));
        when(evaluations.findByAttemptIdAndQuestionId(50L, 7L)).thenReturn(Optional.of(failedEvaluation));

        var response = service.retryJob(80L);

        assertEquals(AiJobStatus.PENDING, response.getStatus());
        assertEquals(0, response.getRetryCount());
        assertEquals(AiJobStatus.PENDING, failedEvaluation.getStatus());
        verify(evaluations).save(failedEvaluation);

        failed.setStatus(AiJobStatus.SUCCEEDED);
        assertEquals(ErrorCode.BAD_REQUEST,
                assertThrows(CommonException.class, () -> service.retryJob(80L)).getErrorCode());
    }

    private AiGradeRequest request() {
        return AiGradeRequest.builder().attemptId(50L).questionId(7L).userAnswerText("A writing answer").build();
    }

    private AiJob job(Long id, AiJobStatus status, int retries) {
        return AiJob.builder().jobId(id).jobType(AiJobType.GRADE_WRITING).status(status)
                .idempotencyKey("GRADE_WRITING:50:7").inputRefType("ATTEMPT_ANSWER").inputRefId(50L)
                .retryCount(retries).maxRetries(3).build();
    }

    private AiEvaluation evaluation(AiJobStatus status) {
        return AiEvaluation.builder().evaluationId(90L).attemptId(50L).questionId(7L)
                .userAnswerText("A writing answer").status(status).build();
    }

    private String validResponse() {
        return "{\"overallScore\":7.5,\"criteria\":[],\"errors\":[],\"strengths\":[],"
                + "\"rewriteSuggestion\":\"\",\"confidence\":0.8}";
    }
}
