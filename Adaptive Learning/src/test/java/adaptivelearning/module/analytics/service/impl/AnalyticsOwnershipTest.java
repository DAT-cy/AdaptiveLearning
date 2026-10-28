package adaptivelearning.module.analytics.service.impl;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import adaptivelearning.config.response.CommonException;
import adaptivelearning.module.analytics.entity.Recommendation;
import adaptivelearning.module.analytics.repository.GapAnalysisRepository;
import adaptivelearning.module.analytics.repository.LearningProgressRepository;
import adaptivelearning.module.analytics.repository.RecommendationRepository;
import adaptivelearning.module.attempts.entity.ExamAttempt;
import adaptivelearning.module.attempts.repository.AttemptAnswerRepository;
import adaptivelearning.module.attempts.repository.ExamAttemptRepository;
import adaptivelearning.module.exams.repository.ExamRepository;
import adaptivelearning.module.exams.repository.ExamQuestionRepository;
import adaptivelearning.module.questions.repository.QuestionRepository;
import adaptivelearning.module.topics.repository.TopicRepository;
import adaptivelearning.module.users.repository.UserMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class AnalyticsOwnershipTest {

    @Test
    void userCannotAnalyzeAnotherUsersAttempt() {
        ExamAttemptRepository attempts = mock(ExamAttemptRepository.class);
        AttemptAnswerRepository answers = mock(AttemptAnswerRepository.class);
        when(attempts.findById(9L)).thenReturn(Optional.of(ExamAttempt.builder().attemptId(9L).userId(2L).build()));
        AnalyticsServiceImpl service = service(attempts, answers, mock(RecommendationRepository.class));

        assertThrows(CommonException.class, () -> service.analyzeAttempt(9L, 1L, false));
        verify(answers, org.mockito.Mockito.never()).findByAttemptId(9L);
    }

    @Test
    void userCannotChangeAnotherUsersRecommendation() {
        RecommendationRepository recommendations = mock(RecommendationRepository.class);
        when(recommendations.findById(7L)).thenReturn(Optional.of(Recommendation.builder()
                .recommendationId(7L).userId(2L).build()));
        AnalyticsServiceImpl service = service(mock(ExamAttemptRepository.class),
                mock(AttemptAnswerRepository.class), recommendations);

        assertThrows(CommonException.class, () -> service.startRecommendation(7L, 1L, false));
        verify(recommendations, org.mockito.Mockito.never()).save(org.mockito.ArgumentMatchers.any());
    }

    private AnalyticsServiceImpl service(ExamAttemptRepository attempts, AttemptAnswerRepository answers,
                                         RecommendationRepository recommendations) {
        return new AnalyticsServiceImpl(answers, attempts, mock(GapAnalysisRepository.class), recommendations,
                mock(LearningProgressRepository.class), new ObjectMapper(), mock(ExamRepository.class),
                mock(ExamQuestionRepository.class), mock(QuestionRepository.class), mock(TopicRepository.class),
                mock(UserMapper.class));
    }
}
