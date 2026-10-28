package adaptivelearning.module.ai.service.impl;

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
import adaptivelearning.module.ai.dto.request.AiGradeRequest;
import adaptivelearning.module.ai.dto.request.RubricRequest;
import adaptivelearning.module.ai.entity.AiEvaluation;
import adaptivelearning.module.ai.entity.Rubric;
import adaptivelearning.module.ai.gateway.AiProvider;
import adaptivelearning.module.ai.repository.AiEvaluationRepository;
import adaptivelearning.module.ai.repository.AiJobRepository;
import adaptivelearning.module.ai.repository.RubricRepository;
import adaptivelearning.module.ai.service.impl.AiServiceImpl;
import adaptivelearning.module.attempts.entity.ExamAttempt;
import adaptivelearning.module.attempts.repository.ExamAttemptRepository;
import adaptivelearning.module.exams.entity.Exam;
import adaptivelearning.module.exams.entity.ExamQuestion;
import adaptivelearning.module.exams.repository.ExamQuestionRepository;
import adaptivelearning.module.exams.repository.ExamRepository;
import adaptivelearning.shared.enums.AiJobStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AiGradingServiceTest {

    private AiEvaluationRepository evaluations;
    private RubricRepository rubrics;
    private AiProvider provider;
    private ExamAttemptRepository attempts;
    private ExamQuestionRepository examQuestions;
    private ExamRepository exams;
    private AiServiceImpl service;

    @BeforeEach
    void setUp() {
        evaluations = mock(AiEvaluationRepository.class);
        rubrics = mock(RubricRepository.class);
        provider = mock(AiProvider.class);
        attempts = mock(ExamAttemptRepository.class);
        examQuestions = mock(ExamQuestionRepository.class);
        exams = mock(ExamRepository.class);
        service = new AiServiceImpl(evaluations, mock(AiJobRepository.class), rubrics, provider,
                new ObjectMapper(), attempts, examQuestions, exams);
        when(evaluations.findByAttemptIdAndQuestionId(50L, 7L)).thenReturn(Optional.empty());
        when(evaluations.save(any(AiEvaluation.class))).thenAnswer(invocation -> {
            AiEvaluation evaluation = invocation.getArgument(0);
            evaluation.setEvaluationId(90L);
            return evaluation;
        });
        when(attempts.findById(50L)).thenReturn(Optional.of(ExamAttempt.builder()
                .attemptId(50L).userId(5L).examId(20L).build()));
        when(examQuestions.findByExamIdAndQuestionId(20L, 7L))
                .thenReturn(Optional.of(ExamQuestion.builder().examId(20L).questionId(7L).build()));
        when(exams.findById(20L)).thenReturn(Optional.of(Exam.builder()
                .examId(20L).certificateId(8L).build()));
    }

    @Test
    void validWritingAnswerUsesActiveMatchingRubricAndStoresValidatedResult() {
        when(rubrics.findByRubricIdAndIsActive(3L, true)).thenReturn(Optional.of(
                Rubric.builder().rubricId(3L).certificateId(8L).skill("WRITING")
                        .criteriaJson("[{\"name\":\"Coherence\",\"maxScore\":9}]").build()));
        when(provider.chatCompletionSync(anyString(), anyString(), anyString(), any(Integer.class)))
                .thenReturn(validResponse());

        var response = service.gradeWriting(request(3L, "My writing answer"));

        assertEquals(90L, response.getEvaluationId());
        assertEquals(7.5, response.getOverallScore());
        assertEquals(0.9, response.getConfidence());
        assertEquals(AiJobStatus.SUCCEEDED, response.getStatus());
        verify(provider).chatCompletionSync(anyString(), anyString(), anyString(), any(Integer.class));
        verify(evaluations).save(any(AiEvaluation.class));
    }

    @Test
    void gradingRejectsQuestionOutsideAttemptBeforeProviderCall() {
        when(examQuestions.findByExamIdAndQuestionId(20L, 7L)).thenReturn(Optional.empty());

        CommonException exception = assertThrows(CommonException.class,
                () -> service.submitGradeJob(request(null, "answer"), 5L, false));

        assertEquals(ErrorCode.BAD_REQUEST, exception.getErrorCode());
        verify(provider, never()).chatCompletionSync(anyString(), anyString(), anyString(), any(Integer.class));
    }

    @Test
    void gradingRejectsForeignAttemptOwner() {
        CommonException exception = assertThrows(CommonException.class,
                () -> service.submitGradeJob(request(null, "answer"), 6L, false));

        assertEquals(ErrorCode.FORBIDDEN_ERROR, exception.getErrorCode());
        verify(provider, never()).chatCompletionSync(anyString(), anyString(), anyString(), any(Integer.class));
    }

    @Test
    void gradingRejectsInactiveOrWrongCertificateRubric() {
        when(rubrics.findByRubricIdAndIsActive(3L, true)).thenReturn(Optional.empty());
        CommonException inactive = assertThrows(CommonException.class,
                () -> service.submitGradeJob(request(3L, "answer"), 5L, false));
        assertEquals(ErrorCode.ENTITY_NOT_FOUND, inactive.getErrorCode());

        when(rubrics.findByRubricIdAndIsActive(3L, true)).thenReturn(Optional.of(
                Rubric.builder().rubricId(3L).certificateId(9L).skill("WRITING").build()));
        CommonException mismatch = assertThrows(CommonException.class,
                () -> service.submitGradeJob(request(3L, "answer"), 5L, false));
        assertEquals(ErrorCode.BAD_REQUEST, mismatch.getErrorCode());
        verify(provider, never()).chatCompletionSync(anyString(), anyString(), anyString(), any(Integer.class));
    }

    @Test
    void invalidRequestAndProviderFailureDoNotPersistEvaluation() {
        assertEquals(ErrorCode.BAD_REQUEST,
                assertThrows(CommonException.class, () -> service.gradeWriting(request(null, " "))).getErrorCode());
        assertEquals(ErrorCode.BAD_REQUEST,
                assertThrows(CommonException.class,
                        () -> service.gradeWriting(request(null, "x".repeat(20001)))).getErrorCode());

        when(provider.chatCompletionSync(anyString(), anyString(), anyString(), any(Integer.class)))
                .thenThrow(new IllegalStateException("provider unavailable"));
        assertThrows(IllegalStateException.class, () -> service.gradeWriting(request(null, "valid answer")));
        verify(evaluations, never()).save(any(AiEvaluation.class));
    }

    @Test
    void malformedProviderOutputIsRejectedBeforeSaving() {
        when(provider.chatCompletionSync(anyString(), anyString(), anyString(), any(Integer.class)))
                .thenReturn("{\"overallScore\":42,\"criteria\":[],\"errors\":[],\"strengths\":[],\"confidence\":2,\"rewriteSuggestion\":\"x\"}");

        assertEquals(ErrorCode.BAD_REQUEST,
                assertThrows(CommonException.class, () -> service.gradeWriting(request(null, "valid answer")))
                        .getErrorCode());
        verify(evaluations, never()).save(any(AiEvaluation.class));
    }

    @Test
    void rubricCriteriaMustBeNonEmptyJsonWithNamedPositiveScores() {
        RubricRequest request = RubricRequest.builder().certificateId(8L).skill("WRITING")
                .name("Writing").criteriaJson("not-json").build();
        assertEquals(ErrorCode.BAD_REQUEST,
                assertThrows(CommonException.class, () -> service.createRubric(request)).getErrorCode());

        request.setCriteriaJson("[{\"name\":\"Coherence\",\"maxScore\":0}]");
        assertEquals(ErrorCode.BAD_REQUEST,
                assertThrows(CommonException.class, () -> service.createRubric(request)).getErrorCode());
        verify(rubrics, never()).save(any(Rubric.class));
    }

    private AiGradeRequest request(Long rubricId, String answer) {
        return AiGradeRequest.builder().attemptId(50L).questionId(7L)
                .rubricId(rubricId).userAnswerText(answer).build();
    }

    private String validResponse() {
        return "{\"overallScore\":7.5,\"criteria\":[{\"name\":\"Coherence\",\"score\":7}],"
                + "\"errors\":[],\"strengths\":[\"Clear argument\"],\"rewriteSuggestion\":\"\",\"confidence\":0.9}";
    }
}
