package adaptivelearning.module.adaptive.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import adaptivelearning.config.response.CommonException;
import adaptivelearning.config.response.ErrorCode;
import adaptivelearning.module.adaptive.dto.request.BktEstimateRequest;
import adaptivelearning.module.adaptive.dto.response.BktEstimateResponse;
import adaptivelearning.module.adaptive.entity.LearnerProfile;
import adaptivelearning.module.adaptive.repository.LearnerProfileRepository;
import adaptivelearning.module.adaptive.service.BktClient;
import adaptivelearning.module.attempts.entity.AttemptAnswer;
import adaptivelearning.module.attempts.entity.ExamAttempt;
import adaptivelearning.module.attempts.repository.AttemptAnswerRepository;
import adaptivelearning.module.attempts.repository.ExamAttemptRepository;
import adaptivelearning.module.exams.entity.Exam;
import adaptivelearning.module.exams.entity.ExamQuestion;
import adaptivelearning.module.exams.repository.ExamQuestionRepository;
import adaptivelearning.module.exams.repository.ExamRepository;
import adaptivelearning.module.questions.entity.Question;
import adaptivelearning.module.questions.repository.QuestionRepository;
import adaptivelearning.shared.enums.AttemptStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AdaptiveServiceImplTest {

    private LearnerProfileRepository profiles;
    private ExamAttemptRepository attempts;
    private AttemptAnswerRepository answers;
    private ExamQuestionRepository examQuestions;
    private ExamRepository exams;
    private QuestionRepository questions;
    private BktClient bkt;
    private AdaptiveServiceImpl service;

    @BeforeEach
    void setUp() {
        profiles = mock(LearnerProfileRepository.class);
        attempts = mock(ExamAttemptRepository.class);
        answers = mock(AttemptAnswerRepository.class);
        examQuestions = mock(ExamQuestionRepository.class);
        exams = mock(ExamRepository.class);
        questions = mock(QuestionRepository.class);
        bkt = mock(BktClient.class);
        service = new AdaptiveServiceImpl(profiles, attempts, answers, examQuestions, exams, questions, bkt,
                new ObjectMapper());
    }

    @Test
    void adaptiveRecommendationsUseExistingAbilityDifficultyBoundariesAndTopThree() {
        LearnerProfile profile = profile(5L, 8L,
                "{\"TOPIC_1\":0.39,\"TOPIC_2\":0.4,\"TOPIC_3\":0.69,\"TOPIC_4\":0.7}", 5);
        when(profiles.findByUserIdAndCertificateId(5L, 8L)).thenReturn(Optional.of(profile));

        var recommendations = service.recommend(5L, 8L);

        assertEquals(3, recommendations.size());
        assertEquals("TOPIC_1", recommendations.get(0).getSkill());
        assertEquals("EASY", recommendations.get(0).getTargetDifficulty());
        assertEquals("TOPIC_2", recommendations.get(1).getSkill());
        assertEquals("MEDIUM", recommendations.get(1).getTargetDifficulty());
        assertEquals("TOPIC_3", recommendations.get(2).getSkill());
        assertEquals("MEDIUM", recommendations.get(2).getTargetDifficulty());
    }

    @Test
    void forecastIsExplicitlyUncalibratedAndUsesHeuristicOnly() {
        LearnerProfile profile = profile(5L, 8L, "{\"A\":0.4,\"B\":0.8}", 5);
        when(profiles.findByUserIdAndCertificateId(5L, 8L)).thenReturn(Optional.of(profile));

        var forecast = service.forecast(5L, 8L);

        assertEquals(59.4, forecast.getForecastScore());
        assertEquals(0.5, forecast.getConfidence());
        assertTrue(forecast.getReasoning().contains("Uncalibrated"));
        assertTrue(forecast.getReasoning().contains("not empirically calibrated"));
    }

    @Test
    void missingAndMalformedAbilityHistoryAreNotPresentedAsCalibratedForecasts() {
        when(profiles.findByUserIdAndCertificateId(5L, 8L)).thenReturn(Optional.empty(),
                Optional.of(profile(5L, 8L, "not-json", 3)));

        var noProfile = service.forecast(5L, 8L);
        var invalidProfile = service.forecast(5L, 8L);

        assertEquals(0.0, noProfile.getForecastScore());
        assertTrue(noProfile.getReasoning().contains("Uncalibrated"));
        assertEquals(0.0, invalidProfile.getForecastScore());
        assertTrue(invalidProfile.getReasoning().contains("Uncalibrated"));
    }

    @Test
    void missingProfileOrAbilitiesReturnGeneralRecommendation() {
        when(profiles.findByUserIdAndCertificateId(5L, 8L)).thenReturn(Optional.empty(),
                Optional.of(profile(5L, 8L, "{}", 0)));

        assertEquals("GENERAL", service.recommend(5L, 8L).get(0).getSkill());
        assertEquals("GENERAL", service.recommend(5L, 8L).get(0).getSkill());
    }

    @Test
    void updatingNonOwnedOrUnsubmittedAttemptIsRejected() {
        when(attempts.findById(90L)).thenReturn(Optional.empty());
        CommonException missing = assertThrows(CommonException.class, () -> service.updateProfile(5L, 90L));
        assertEquals(ErrorCode.ENTITY_NOT_FOUND, missing.getErrorCode());

        when(attempts.findById(91L)).thenReturn(Optional.of(completedAttempt(91L, 6L, AttemptStatus.GRADED)));
        CommonException foreign = assertThrows(CommonException.class, () -> service.updateProfile(5L, 91L));
        assertEquals(ErrorCode.ENTITY_NOT_FOUND, foreign.getErrorCode());

        when(attempts.findById(92L)).thenReturn(Optional.of(completedAttempt(92L, 5L, AttemptStatus.IN_PROGRESS)));
        CommonException incomplete = assertThrows(CommonException.class, () -> service.updateProfile(5L, 92L));
        assertEquals(ErrorCode.BAD_REQUEST, incomplete.getErrorCode());
        verify(bkt, never()).estimate(any(BktEstimateRequest.class));
    }

    @Test
    void heuristicProfileUpdateSurvivesUnavailableOrNotReadyBkt() {
        ExamAttempt attempt = completedAttempt(93L, 5L, AttemptStatus.GRADED);
        when(attempts.findById(93L)).thenReturn(Optional.of(attempt));
        when(exams.findById(20L)).thenReturn(Optional.of(Exam.builder().examId(20L).certificateId(8L).build()));
        when(examQuestions.findByExamId(20L)).thenReturn(List.of(ExamQuestion.builder()
                .examId(20L).questionId(1L).questionOrder(1).build(), ExamQuestion.builder()
                .examId(20L).questionId(2L).questionOrder(2).build()));
        Question objective = Question.builder().questionId(1L).certificateId(8L).topicId(3L)
                .questionType("MULTIPLE_CHOICE").build();
        Question essay = Question.builder().questionId(2L).certificateId(8L).topicId(4L)
                .questionType("ESSAY").build();
        when(questions.findAllById(anyIterable())).thenReturn(List.of(objective, essay));
        when(answers.findByAttemptId(93L)).thenReturn(List.of(
                AttemptAnswer.builder().attemptId(93L).questionId(1L).isCorrect(true).build(),
                AttemptAnswer.builder().attemptId(93L).questionId(2L).isCorrect(null).build()));
        when(answers.findByAttemptIdIn(any())).thenReturn(List.of(
                AttemptAnswer.builder().attemptId(93L).questionId(1L).isCorrect(true).build(),
                AttemptAnswer.builder().attemptId(93L).questionId(2L).isCorrect(null).build()));
        when(attempts.findByStatusIn(any())).thenReturn(List.of(attempt));
        when(exams.findAllById(anyIterable())).thenReturn(List.of(
                Exam.builder().examId(20L).certificateId(8L).build()));
        when(profiles.findByUserIdAndCertificateId(5L, 8L)).thenReturn(Optional.empty());
        when(profiles.save(any(LearnerProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(bkt.estimate(any(BktEstimateRequest.class))).thenReturn(Optional.empty());

        var updated = service.updateProfile(5L, 93L);

        assertTrue(updated.getAbilityBySkillJson().contains("TOPIC_3"));
        assertEquals(1, updated.getTotalAttempts());
        var requestCaptor = org.mockito.ArgumentCaptor.forClass(BktEstimateRequest.class);
        verify(bkt).estimate(requestCaptor.capture());
        assertEquals(1, requestCaptor.getValue().getLearnerInteractions().size());
        verify(profiles).save(any(LearnerProfile.class));
    }

    private LearnerProfile profile(Long userId, Long certificateId, String abilities, int totalAttempts) {
        return LearnerProfile.builder().profileId(1L).userId(userId).certificateId(certificateId)
                .abilityBySkillJson(abilities).totalAttempts(totalAttempts).build();
    }

    private ExamAttempt completedAttempt(Long id, Long userId, AttemptStatus status) {
        return ExamAttempt.builder().attemptId(id).userId(userId).examId(20L).status(status)
                .startedAt(LocalDateTime.now().minusMinutes(5)).submittedAt(LocalDateTime.now())
                .score(1d).maxScore(1d).build();
    }
}
