package adaptivelearning.module.analytics.service.impl;

import adaptivelearning.module.analytics.entity.LearningProgress;
import adaptivelearning.module.analytics.repository.GapAnalysisRepository;
import adaptivelearning.module.analytics.repository.LearningProgressRepository;
import adaptivelearning.module.analytics.repository.RecommendationRepository;
import adaptivelearning.module.attempts.entity.ExamAttempt;
import adaptivelearning.module.attempts.repository.AttemptAnswerRepository;
import adaptivelearning.module.attempts.repository.ExamAttemptRepository;
import adaptivelearning.module.exams.entity.Exam;
import adaptivelearning.module.exams.repository.ExamRepository;
import adaptivelearning.module.exams.repository.ExamQuestionRepository;
import adaptivelearning.module.questions.repository.QuestionRepository;
import adaptivelearning.module.topics.entity.Topic;
import adaptivelearning.module.topics.repository.TopicRepository;
import adaptivelearning.module.users.entity.User;
import adaptivelearning.module.users.entity.Role;
import adaptivelearning.module.users.repository.UserMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AnalyticsServiceImplTest {

    @Test
    void progressOverviewAggregatesSubmittedScoresForSelectedCertificate() {
        AttemptAnswerRepository answerRepository = mock(AttemptAnswerRepository.class);
        ExamAttemptRepository attemptRepository = mock(ExamAttemptRepository.class);
        GapAnalysisRepository gapAnalysisRepository = mock(GapAnalysisRepository.class);
        RecommendationRepository recommendationRepository = mock(RecommendationRepository.class);
        LearningProgressRepository progressRepository = mock(LearningProgressRepository.class);
        ExamRepository examRepository = mock(ExamRepository.class);
        TopicRepository topicRepository = mock(TopicRepository.class);
        UserMapper userMapper = mock(UserMapper.class);
        LocalDateTime submittedAt = LocalDateTime.of(2026, 10, 20, 10, 0);

        when(progressRepository.findByUserIdAndCertificateIdOrderBySnapshotAtDesc(5L, 3L))
                .thenReturn(List.<LearningProgress>of());
        when(attemptRepository.findByUserIdOrderByStartedAtDesc(5L)).thenReturn(List.of(
                attempt(12L, 2L, 80.0, 100.0, submittedAt.plusDays(1)),
                attempt(11L, 1L, 60.0, 100.0, submittedAt),
                attempt(10L, 3L, 100.0, 100.0, submittedAt.plusDays(2))));
        when(examRepository.findAllById(anyIterable())).thenReturn(List.of(
                exam(1L, 3L, 7L, "Listening Mock 1"),
                exam(2L, 3L, 7L, "Listening Mock 2"),
                exam(3L, 4L, 8L, "Other exam")));
        when(topicRepository.findAllById(anyIterable())).thenReturn(List.of(
                Topic.builder().topicId(7L).name("Listening").build()));

        AnalyticsServiceImpl service = new AnalyticsServiceImpl(
                answerRepository, attemptRepository, gapAnalysisRepository, recommendationRepository,
                progressRepository, new ObjectMapper(), examRepository, mock(ExamQuestionRepository.class),
                mock(QuestionRepository.class), topicRepository, userMapper);

        var progress = service.getProgressOverview(5L, 3L);

        assertEquals(2, progress.getTotalAttempts());
        assertEquals(70.0, progress.getAverageScore(), 0.001);
        assertEquals(2, progress.getRecentAttempts().size());
        assertEquals("Listening Mock 2", progress.getRecentAttempts().get(0).getExamTitle());
        assertTrue(progress.getMasteryBySkillJson().contains("\"Listening\":70.0"));
        assertTrue(progress.getScoreTrendJson().contains("\"scorePercent\":60.0"));
    }

    @Test
    void adminOverviewReturnsPeriodComparisonAndLowScoreLearners() {
        AttemptAnswerRepository answerRepository = mock(AttemptAnswerRepository.class);
        ExamAttemptRepository attemptRepository = mock(ExamAttemptRepository.class);
        GapAnalysisRepository gapAnalysisRepository = mock(GapAnalysisRepository.class);
        RecommendationRepository recommendationRepository = mock(RecommendationRepository.class);
        LearningProgressRepository progressRepository = mock(LearningProgressRepository.class);
        ExamRepository examRepository = mock(ExamRepository.class);
        TopicRepository topicRepository = mock(TopicRepository.class);
        UserMapper userMapper = mock(UserMapper.class);
        LocalDate today = LocalDate.now();
        LocalDateTime currentDate = today.minusDays(1).atTime(10, 0);
        LocalDateTime previousDate = today.minusDays(35).atTime(10, 0);

        when(attemptRepository.findByStartedAtGreaterThanEqualAndStartedAtLessThan(any(), any()))
                .thenReturn(List.of(
                        attempt(21L, 1L, 80.0, 100.0, currentDate),
                        ExamAttempt.builder().attemptId(22L).userId(6L).examId(1L)
                                .startedAt(today.atTime(11, 0)).maxScore(100.0).build(),
                        attempt(20L, 1L, 50.0, 100.0, previousDate)));
        when(examRepository.findAllById(anyIterable())).thenReturn(List.of(
                exam(1L, 3L, 7L, "Reading Mock")));
        User learner = new User();
        learner.setUserId(5L);
        learner.setUsername("learner05");
        learner.setRole(Role.ROLE_USER);
        User secondLearner = new User();
        secondLearner.setUserId(6L);
        secondLearner.setUsername("learner06");
        secondLearner.setRole(Role.ROLE_USER);
        when(userMapper.findAllForAdmin()).thenReturn(List.of(learner, secondLearner));

        AnalyticsServiceImpl service = new AnalyticsServiceImpl(
                answerRepository, attemptRepository, gapAnalysisRepository, recommendationRepository,
                progressRepository, new ObjectMapper(), examRepository, mock(ExamQuestionRepository.class),
                mock(QuestionRepository.class), topicRepository, userMapper);

        var report = service.getAdminOverview("30d", 3L);

        assertEquals(2, report.startedAttempts());
        assertEquals(1, report.completedAttempts());
        assertEquals(50.0, report.completionRate(), 0.001);
        assertEquals(-50.0, report.completionRateChange(), 0.001);
        assertEquals(80.0, report.averageScorePercent(), 0.001);
        assertEquals(30.0, report.averageScoreChange(), 0.001);
        assertEquals(2, report.activeLearners());
        assertEquals("learner05", report.learnersToSupport().get(0).username());
        assertEquals(5, report.trend().size());
    }

    private ExamAttempt attempt(Long id, Long examId, Double score, Double maxScore, LocalDateTime submittedAt) {
        return ExamAttempt.builder()
                .attemptId(id)
                .userId(5L)
                .examId(examId)
                .score(score)
                .maxScore(maxScore)
                .startedAt(submittedAt.minusHours(1))
                .submittedAt(submittedAt)
                .build();
    }

    private Exam exam(Long id, Long certificateId, Long topicId, String title) {
        return Exam.builder()
                .examId(id)
                .certificateId(certificateId)
                .topicId(topicId)
                .title(title)
                .build();
    }
}
