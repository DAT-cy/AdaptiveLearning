package adaptivelearning.module.speaking.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import adaptivelearning.config.response.CommonException;
import adaptivelearning.config.response.ErrorCode;
import adaptivelearning.module.ai.entity.Rubric;
import adaptivelearning.module.ai.gateway.AiProvider;
import adaptivelearning.module.ai.repository.RubricRepository;
import adaptivelearning.module.attempts.entity.ExamAttempt;
import adaptivelearning.module.attempts.repository.ExamAttemptRepository;
import adaptivelearning.module.exams.entity.Exam;
import adaptivelearning.module.exams.entity.ExamQuestion;
import adaptivelearning.module.exams.repository.ExamQuestionRepository;
import adaptivelearning.module.exams.repository.ExamRepository;
import adaptivelearning.module.questions.repository.QuestionRepository;
import adaptivelearning.module.speaking.dto.request.SpeakingSubmissionRequest;
import adaptivelearning.module.speaking.entity.SpeakingEvaluation;
import adaptivelearning.module.speaking.entity.SpeakingSubmission;
import adaptivelearning.module.speaking.repository.SpeakingEvaluationRepository;
import adaptivelearning.module.speaking.repository.SpeakingSubmissionRepository;
import adaptivelearning.shared.enums.SpeakingStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SpeakingServiceImplTest {

    private SpeakingSubmissionRepository submissions;
    private SpeakingEvaluationRepository evaluations;
    private AiProvider ai;
    private ExamAttemptRepository attempts;
    private ExamQuestionRepository examQuestions;
    private ExamRepository exams;
    private QuestionRepository questions;
    private RubricRepository rubrics;
    private SpeakingServiceImpl service;

    @BeforeEach
    void setUp() {
        submissions = mock(SpeakingSubmissionRepository.class);
        evaluations = mock(SpeakingEvaluationRepository.class);
        ai = mock(AiProvider.class);
        attempts = mock(ExamAttemptRepository.class);
        examQuestions = mock(ExamQuestionRepository.class);
        exams = mock(ExamRepository.class);
        questions = mock(QuestionRepository.class);
        rubrics = mock(RubricRepository.class);
        service = new SpeakingServiceImpl(submissions, evaluations, ai,
                new com.fasterxml.jackson.databind.ObjectMapper(), attempts, examQuestions, exams,
                questions, rubrics);
        when(submissions.save(any(SpeakingSubmission.class))).thenAnswer(invocation -> {
            SpeakingSubmission submission = invocation.getArgument(0);
            if (submission.getSubmissionId() == null) submission.setSubmissionId(12L);
            return submission;
        });
        when(evaluations.save(any(SpeakingEvaluation.class))).thenAnswer(invocation -> {
            SpeakingEvaluation evaluation = invocation.getArgument(0);
            if (evaluation.getEvaluationId() == null) evaluation.setEvaluationId(40L);
            return evaluation;
        });
    }

    @Test
    void submitRequiresCallerOwnedAttemptAndQuestionBelongingToExam() {
        when(attempts.findById(7L)).thenReturn(Optional.of(attempt(7L, 5L, 20L)));
        SpeakingSubmissionRequest request = submissionRequest(7L, 9L, "transcript");
        when(examQuestions.findByExamIdAndQuestionId(20L, 9L)).thenReturn(Optional.empty());
        assertEquals(ErrorCode.BAD_REQUEST,
                assertThrows(CommonException.class, () -> service.submit(5L, request)).getErrorCode());

        when(attempts.findById(7L)).thenReturn(Optional.of(attempt(7L, 6L, 20L)));
        assertEquals(ErrorCode.ENTITY_NOT_FOUND,
                assertThrows(CommonException.class, () -> service.submit(5L, request)).getErrorCode());
        verify(submissions, never()).save(any(SpeakingSubmission.class));
    }

    @Test
    void submitStoresTranscriptMvpWithoutClaimingAudioProcessing() {
        when(attempts.findById(7L)).thenReturn(Optional.of(attempt(7L, 5L, 20L)));
        when(examQuestions.findByExamIdAndQuestionId(20L, 9L)).thenReturn(Optional.of(
                ExamQuestion.builder().examId(20L).questionId(9L).build()));
        when(questions.existsById(9L)).thenReturn(true);

        var response = service.submit(5L, submissionRequest(7L, 9L, "  transcript  "));

        assertEquals(12L, response.getSubmissionId());
        assertEquals("transcript", response.getTranscriptText());
        assertEquals(SpeakingStatus.UPLOADED, response.getStatus());
        verify(ai, never()).chatCompletionSync(anyString(), anyString(), anyString(), org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    void evaluateUsesMatchingSpeakingRubricAndStoresStructuredAnswerAndCitationFields() {
        SpeakingSubmission submission = submission(5L);
        when(submissions.findBySubmissionIdAndUserId(12L, 5L)).thenReturn(Optional.of(submission));
        when(rubrics.findByRubricIdAndIsActive(3L, true)).thenReturn(Optional.of(Rubric.builder()
                .rubricId(3L).certificateId(8L).skill("SPEAKING")
                .criteriaJson("[{\"name\":\"Fluency\",\"maxScore\":9}]").build()));
        when(attempts.findById(7L)).thenReturn(Optional.of(attempt(7L, 5L, 20L)));
        when(exams.findById(20L)).thenReturn(Optional.of(Exam.builder().examId(20L).certificateId(8L).build()));
        when(evaluations.findBySubmissionIdAndQuestionId(12L, 9L)).thenReturn(List.of());
        when(ai.chatCompletionSync(anyString(), anyString(), anyString(), org.mockito.ArgumentMatchers.anyInt()))
                .thenReturn(validResponse());

        var response = service.evaluate(5L, 12L, 3L);

        assertEquals(40L, response.getEvaluationId());
        assertEquals(8.5, response.getOverallScore());
        assertEquals("gpt-4o-mini", response.getModel());
        assertEquals(SpeakingStatus.GRADED, submission.getStatus());
        verify(ai).chatCompletionSync(anyString(), org.mockito.ArgumentMatchers.contains("Fluency"),
                org.mockito.ArgumentMatchers.eq("gpt-4o-mini"), org.mockito.ArgumentMatchers.eq(1200));
    }

    @Test
    void evaluationRejectsForeignOwnerAndMismatchedRubricBeforeProvider() {
        when(submissions.findBySubmissionIdAndUserId(12L, 6L)).thenReturn(Optional.empty());
        assertEquals(ErrorCode.ENTITY_NOT_FOUND,
                assertThrows(CommonException.class, () -> service.evaluate(6L, 12L, null)).getErrorCode());

        when(submissions.findBySubmissionIdAndUserId(12L, 5L)).thenReturn(Optional.of(submission(5L)));
        when(rubrics.findByRubricIdAndIsActive(3L, true)).thenReturn(Optional.of(Rubric.builder()
                .rubricId(3L).certificateId(8L).skill("WRITING").build()));
        when(attempts.findById(7L)).thenReturn(Optional.of(attempt(7L, 5L, 20L)));
        when(exams.findById(20L)).thenReturn(Optional.of(Exam.builder().examId(20L).certificateId(8L).build()));
        assertEquals(ErrorCode.BAD_REQUEST,
                assertThrows(CommonException.class, () -> service.evaluate(5L, 12L, 3L)).getErrorCode());
        verify(ai, never()).chatCompletionSync(anyString(), anyString(), anyString(), org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    void evaluationLookupUsesEvaluationIdThenChecksSubmissionOwner() {
        when(evaluations.findById(40L)).thenReturn(Optional.of(SpeakingEvaluation.builder()
                .evaluationId(40L).submissionId(12L).questionId(9L).overallScore(8.0).build()));
        when(submissions.findBySubmissionIdAndUserId(12L, 5L)).thenReturn(Optional.of(submission(5L)));
        when(submissions.findBySubmissionIdAndUserId(12L, 6L)).thenReturn(Optional.empty());

        assertEquals(40L, service.getEvaluation(5L, 40L).getEvaluationId());
        assertEquals(ErrorCode.ENTITY_NOT_FOUND,
                assertThrows(CommonException.class, () -> service.getEvaluation(6L, 40L)).getErrorCode());
    }

    @Test
    void providerFailureAndMalformedEvaluationReturnSafeErrors() {
        when(submissions.findBySubmissionIdAndUserId(12L, 5L)).thenReturn(Optional.of(submission(5L)));
        when(ai.chatCompletionSync(anyString(), anyString(), anyString(), org.mockito.ArgumentMatchers.anyInt()))
                .thenThrow(new IllegalStateException("sensitive transcript/provider detail"));
        assertEquals(ErrorCode.AI_PROVIDER_UNAVAILABLE,
                assertThrows(CommonException.class, () -> service.evaluate(5L, 12L, null)).getErrorCode());

        org.mockito.Mockito.doReturn("not-json").when(ai).chatCompletionSync(anyString(), anyString(),
                anyString(), org.mockito.ArgumentMatchers.anyInt());
        assertEquals(ErrorCode.AI_PROVIDER_INVALID_RESPONSE,
                assertThrows(CommonException.class, () -> service.evaluate(5L, 12L, null)).getErrorCode());
    }

    private SpeakingSubmissionRequest submissionRequest(Long attemptId, Long questionId, String transcript) {
        SpeakingSubmissionRequest request = new SpeakingSubmissionRequest();
        request.setAttemptId(attemptId);
        request.setQuestionId(questionId);
        request.setTranscriptText(transcript);
        return request;
    }

    private SpeakingSubmission submission(Long userId) {
        return SpeakingSubmission.builder().submissionId(12L).attemptId(7L).userId(userId)
                .questionId(9L).transcriptText("spoken words").status(SpeakingStatus.UPLOADED).build();
    }

    private ExamAttempt attempt(Long id, Long userId, Long examId) {
        return ExamAttempt.builder().attemptId(id).userId(userId).examId(examId).build();
    }

    private String validResponse() {
        return "{\"overallScore\":8.5,\"criteria\":[],\"errors\":[],\"transcript\":[],"
                + "\"strengths\":[\"clear\"],\"rewriteSuggestion\":\"\",\"confidence\":0.9}";
    }
}
