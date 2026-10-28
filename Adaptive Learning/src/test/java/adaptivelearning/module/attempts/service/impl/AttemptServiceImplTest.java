package adaptivelearning.module.attempts.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import adaptivelearning.config.response.CommonException;
import adaptivelearning.config.response.ErrorCode;
import adaptivelearning.module.attempts.dto.request.SaveAnswerRequest;
import adaptivelearning.module.attempts.dto.request.SubmitAttemptRequest;
import adaptivelearning.module.attempts.entity.AttemptAnswer;
import adaptivelearning.module.attempts.entity.ExamAttempt;
import adaptivelearning.module.attempts.repository.AttemptAnswerRepository;
import adaptivelearning.module.attempts.repository.AttemptEventRepository;
import adaptivelearning.module.attempts.repository.ExamAttemptRepository;
import adaptivelearning.module.billing.service.EntitlementService;
import adaptivelearning.module.exams.entity.Exam;
import adaptivelearning.module.exams.entity.ExamQuestion;
import adaptivelearning.module.exams.repository.ExamQuestionRepository;
import adaptivelearning.module.exams.repository.ExamRepository;
import adaptivelearning.module.questions.entity.Question;
import adaptivelearning.module.questions.repository.QuestionRepository;
import adaptivelearning.shared.enums.AttemptStatus;
import adaptivelearning.shared.enums.ContentStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AttemptServiceImplTest {

    private ExamAttemptRepository attempts;
    private AttemptAnswerRepository answers;
    private AttemptEventRepository events;
    private ExamRepository exams;
    private ExamQuestionRepository examQuestions;
    private QuestionRepository questions;
    private EntitlementService entitlements;
    private AttemptServiceImpl service;

    @BeforeEach
    void setUp() {
        attempts = mock(ExamAttemptRepository.class);
        answers = mock(AttemptAnswerRepository.class);
        events = mock(AttemptEventRepository.class);
        exams = mock(ExamRepository.class);
        examQuestions = mock(ExamQuestionRepository.class);
        questions = mock(QuestionRepository.class);
        entitlements = mock(EntitlementService.class);
        service = new AttemptServiceImpl(attempts, answers, events, exams, examQuestions,
                questions, new ObjectMapper(), entitlements);
    }

    @Test
    void startRequiresPublishedExamAndChecksFreeQuotaBeforeCreatingAttempt() {
        Exam draft = exam(10L, 4L, ContentStatus.DRAFT);
        when(exams.findByIdForUpdate(10L)).thenReturn(Optional.of(draft));
        assertThrows(CommonException.class, () -> service.startAttempt(4L, 10L));
        verify(attempts, never()).save(any(ExamAttempt.class));

        Exam published = exam(10L, 4L, ContentStatus.PUBLISHED);
        when(exams.findByIdForUpdate(11L)).thenReturn(Optional.of(published));
        when(entitlements.hasPremiumAccess(4L)).thenReturn(false);
        when(attempts.existsByUserIdAndExamIdAndSubmittedAtIsNotNull(4L, 11L)).thenReturn(true);
        CommonException quota = assertThrows(CommonException.class, () -> service.startAttempt(4L, 11L));
        assertEquals(ErrorCode.FREE_LIMIT_REACHED, quota.getErrorCode());
        verify(attempts, never()).save(any(ExamAttempt.class));
    }

    @Test
    void startCreatesOwnedInProgressAttemptWithServerDurationAndMaxScore() {
        Exam published = exam(10L, null, ContentStatus.PUBLISHED);
        when(exams.findByIdForUpdate(10L)).thenReturn(Optional.of(published));
        when(attempts.findByUserIdAndExamId(4L, 10L)).thenReturn(Optional.empty());
        when(attempts.save(any(ExamAttempt.class))).thenAnswer(invocation -> {
            ExamAttempt attempt = invocation.getArgument(0);
            attempt.setAttemptId(21L);
            return attempt;
        });

        var response = service.startAttempt(4L, 10L);

        assertEquals(21L, response.getAttemptId());
        assertEquals(AttemptStatus.IN_PROGRESS, response.getStatus());
        assertEquals(4L, response.getUserId());
        assertEquals(10L, response.getExamId());
        assertEquals(50.0, response.getMaxScore());
    }

    @Test
    void saveAnswerUpsertsExistingAnswerAndRejectsQuestionOutsideExam() {
        ExamAttempt attempt = attempt(21L, 4L, 10L, AttemptStatus.IN_PROGRESS);
        when(attempts.findByIdForUpdate(21L)).thenReturn(Optional.of(attempt));
        ExamQuestion mapping = examQuestion(10L, 30L, null);
        when(examQuestions.findByExamIdAndQuestionId(10L, 30L)).thenReturn(Optional.of(mapping));
        AttemptAnswer existing = AttemptAnswer.builder().id(5L).attemptId(21L).questionId(30L)
                .userAnswerJson("\"A\"").timeSpentSeconds(3).build();
        when(answers.findByAttemptIdAndQuestionId(21L, 30L)).thenReturn(Optional.of(existing));

        service.saveAnswer(21L, 4L, answer(30L, "\"B\"", 12));

        assertEquals("\"B\"", existing.getUserAnswerJson());
        assertEquals(12, existing.getTimeSpentSeconds());
        verify(answers).save(existing);
        verify(events).save(any());

        when(examQuestions.findByExamIdAndQuestionId(10L, 99L)).thenReturn(Optional.empty());
        assertThrows(CommonException.class, () -> service.saveAnswer(21L, 4L, answer(99L, "\"A\"", 1)));
    }

    @Test
    void expiredWritePersistsExpiredStateAndReturnsBadRequest() {
        ExamAttempt attempt = attempt(21L, 4L, 10L, AttemptStatus.IN_PROGRESS);
        attempt.setExpiresAt(LocalDateTime.now().minusSeconds(1));
        when(attempts.findByIdForUpdate(21L)).thenReturn(Optional.of(attempt));

        CommonException exception = assertThrows(CommonException.class,
                () -> service.saveAnswer(21L, 4L, answer(30L, "\"A\"", 1)));

        assertEquals(ErrorCode.ATTEMPT_EXPIRED, exception.getErrorCode());
        assertEquals(AttemptStatus.EXPIRED, attempt.getStatus());
        verify(attempts).save(attempt);
        verify(answers, never()).save(any(AttemptAnswer.class));
    }

    @Test
    void submitScoresSupportedTypesAndOverridesAndDuplicateReturnsSameResult() {
        ExamAttempt attempt = attempt(21L, 4L, 10L, AttemptStatus.IN_PROGRESS);
        attempt.setMaxScore(16d);
        when(attempts.findByIdForUpdate(21L)).thenReturn(Optional.of(attempt));
        when(examQuestions.findByExamIdAndQuestionId(10L, 30L))
                .thenReturn(Optional.of(examQuestion(10L, 30L, 2d)));
        when(examQuestions.findByExamIdAndQuestionId(10L, 31L))
                .thenReturn(Optional.of(examQuestion(10L, 31L, 8d)));
        when(examQuestions.findByExamIdAndQuestionId(10L, 32L))
                .thenReturn(Optional.of(examQuestion(10L, 32L, 3d)));
        when(examQuestions.findByExamIdAndQuestionId(10L, 33L))
                .thenReturn(Optional.of(examQuestion(10L, 33L, 2d)));
        when(answers.findByAttemptIdAndQuestionId(21L, 30L)).thenReturn(Optional.empty());
        List<AttemptAnswer> storedAnswers = new ArrayList<>();
        when(answers.save(any(AttemptAnswer.class))).thenAnswer(invocation -> {
            AttemptAnswer answer = invocation.getArgument(0);
            storedAnswers.removeIf(existing -> existing.getQuestionId().equals(answer.getQuestionId()));
            storedAnswers.add(answer);
            return answer;
        });
        when(examQuestions.findByExamId(10L)).thenReturn(List.of(
                examQuestion(10L, 30L, 2d), examQuestion(10L, 31L, 8d), examQuestion(10L, 32L, 3d),
                examQuestion(10L, 33L, 2d), examQuestion(10L, 34L, 1d)));
        Question choice = Question.builder().questionId(30L).questionType("MULTIPLE_CHOICE")
                .correctAnswer("\"A\"").points(5d).build();
        Question essay = Question.builder().questionId(31L).questionType("ESSAY")
                .correctAnswer(null).points(8d).build();
        Question fillBlank = Question.builder().questionId(32L).questionType("FILL_BLANK")
                .correctAnswer("Blue").points(3d).build();
        Question wrongChoice = Question.builder().questionId(33L).questionType("MULTIPLE_CHOICE")
                .correctAnswer("\"B\"").points(2d).build();
        Question unansweredChoice = Question.builder().questionId(34L).questionType("MULTIPLE_CHOICE")
                .correctAnswer("\"C\"").points(1d).build();
        when(questions.findAllById(anyIterable())).thenReturn(List.of(choice, essay, fillBlank,
                wrongChoice, unansweredChoice));
        when(answers.findByAttemptId(21L)).thenAnswer(invocation -> new ArrayList<>(storedAnswers));

        SubmitAttemptRequest request = new SubmitAttemptRequest();
        request.setAnswers(List.of(answer(30L, "\"A\"", 20), answer(31L, "\"essay text\"", 30),
                answer(32L, "\"blue\"", 15), answer(33L, "\"A\"", 10)));

        var submission = service.submitAttemptWithOutcome(21L, 4L, request);
        var result = submission.result();

        org.junit.jupiter.api.Assertions.assertTrue(submission.newlySubmitted());
        assertEquals(5d, result.getScore());
        assertEquals(16d, result.getMaxScore());
        assertEquals(5, result.getTotalQuestions());
        assertEquals(2, result.getCorrectCount());
        assertEquals(1, result.getWrongCount());
        assertEquals(1, result.getUnansweredCount());
        assertEquals(AttemptStatus.GRADED, attempt.getStatus());
        assertNull(storedAnswers.stream().filter(a -> a.getQuestionId().equals(31L))
                .findFirst().orElseThrow().getIsCorrect());
        int eventWritesAfterSubmit = org.mockito.Mockito.mockingDetails(events).getInvocations().size();

        var duplicate = service.submitAttemptWithOutcome(21L, 4L, request);

        org.junit.jupiter.api.Assertions.assertFalse(duplicate.newlySubmitted());
        assertEquals(result, duplicate.result());
        assertEquals(eventWritesAfterSubmit, org.mockito.Mockito.mockingDetails(events).getInvocations().size());
        assertEquals(AttemptStatus.GRADED, attempt.getStatus());
    }

    @Test
    void submitRejectsForeignQuestionAndForeignOwner() {
        ExamAttempt attempt = attempt(21L, 4L, 10L, AttemptStatus.IN_PROGRESS);
        when(attempts.findByIdForUpdate(21L)).thenReturn(Optional.of(attempt));
        CommonException foreignOwner = assertThrows(CommonException.class,
                () -> service.submitAttempt(21L, 7L, new SubmitAttemptRequest()));
        assertEquals(ErrorCode.FORBIDDEN_ERROR, foreignOwner.getErrorCode());

        when(examQuestions.findByExamIdAndQuestionId(10L, 99L)).thenReturn(Optional.empty());
        SubmitAttemptRequest request = new SubmitAttemptRequest();
        request.setAnswers(List.of(answer(99L, "\"A\"", 1)));
        assertThrows(CommonException.class, () -> service.submitAttempt(21L, 4L, request));
        assertEquals(AttemptStatus.IN_PROGRESS, attempt.getStatus());
        verify(events, never()).save(any());
    }

    @Test
    void expiryJobTransitionsOnlyLockedOverdueAttempts() {
        ExamAttempt overdue = attempt(21L, 4L, 10L, AttemptStatus.IN_PROGRESS);
        overdue.setExpiresAt(LocalDateTime.now().minusSeconds(1));
        when(attempts.findOverdueForUpdate(any(), any())).thenReturn(List.of(overdue));

        service.expireOverdueAttempts();

        assertEquals(AttemptStatus.EXPIRED, overdue.getStatus());
        verify(attempts).saveAll(List.of(overdue));
    }

    @Test
    void violationThresholdKeepsViolatedTerminalStatusAfterAutoScoring() {
        ExamAttempt attempt = attempt(21L, 4L, 10L, AttemptStatus.IN_PROGRESS);
        attempt.setViolationCount(4);
        when(attempts.findByIdForUpdate(21L)).thenReturn(Optional.of(attempt));
        when(examQuestions.findByExamId(10L)).thenReturn(List.of(examQuestion(10L, 30L, null)));
        when(questions.findAllById(anyIterable())).thenReturn(List.of());
        when(answers.findByAttemptId(21L)).thenReturn(List.of());

        service.recordViolation(21L, 4L, false, new adaptivelearning.module.attempts.dto.request.ViolationRequest());

        assertEquals(AttemptStatus.VIOLATED, attempt.getStatus());
        assertEquals(5, attempt.getViolationCount());
        assertEquals(0d, attempt.getScore());
        assertEquals(1, attempt.getSubmittedAt() == null ? 0 : 1);
    }

    private Exam exam(Long id, Long owner, ContentStatus status) {
        return Exam.builder().examId(id).ownerUserId(owner).topicId(3L).status(status)
                .durationMinutes(30).totalPoints(50d).build();
    }

    private ExamAttempt attempt(Long id, Long userId, Long examId, AttemptStatus status) {
        return ExamAttempt.builder().attemptId(id).userId(userId).examId(examId).status(status)
                .startedAt(LocalDateTime.now().minusMinutes(1)).expiresAt(LocalDateTime.now().plusMinutes(10))
                .build();
    }

    private ExamQuestion examQuestion(Long examId, Long questionId, Double pointsOverride) {
        return ExamQuestion.builder().examId(examId).questionId(questionId).pointsOverride(pointsOverride).build();
    }

    private SaveAnswerRequest answer(Long questionId, String value, int timeSpent) {
        return SaveAnswerRequest.builder().questionId(questionId).userAnswerJson(value)
                .timeSpentSeconds(timeSpent).build();
    }
}
