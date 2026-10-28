package adaptivelearning.module.analytics.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import adaptivelearning.config.response.CommonException;
import adaptivelearning.config.response.ErrorCode;
import adaptivelearning.module.analytics.entity.GapAnalysis;
import adaptivelearning.module.analytics.entity.LearningProgress;
import adaptivelearning.module.analytics.entity.Recommendation;
import adaptivelearning.module.analytics.repository.GapAnalysisRepository;
import adaptivelearning.module.analytics.repository.LearningProgressRepository;
import adaptivelearning.module.analytics.repository.RecommendationRepository;
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
import adaptivelearning.module.topics.entity.Topic;
import adaptivelearning.module.topics.repository.TopicRepository;
import adaptivelearning.module.users.repository.UserMapper;
import adaptivelearning.shared.enums.QuestionDifficulty;
import adaptivelearning.shared.enums.RecommendationPriority;
import adaptivelearning.shared.enums.TopicType;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class AnalyticsAnalysisTest {

    @Test
    void analysisGroupsOnlyScoredExamAnswersBySkillTopicAndDifficulty() throws Exception {
        Fixture fixture = new Fixture(3);
        fixture.examQuestions(List.of(
                mapping(1L), mapping(2L), mapping(3L), mapping(4L)));
        fixture.questions(List.of(
                question(1L, 10L, "MULTIPLE_CHOICE", QuestionDifficulty.EASY),
                question(2L, 11L, "MULTIPLE_CHOICE", QuestionDifficulty.HARD),
                question(3L, 10L, "ESSAY", QuestionDifficulty.EASY),
                question(4L, 12L, "MULTIPLE_CHOICE", QuestionDifficulty.MEDIUM)));
        fixture.topics(List.of(
                topic(10L, "Reading", TopicType.SKILL, null),
                topic(11L, "Inference", TopicType.TOPIC, 10L),
                topic(12L, "Writing", TopicType.SKILL, null)));
        fixture.answers(List.of(
                answer(1L, true), answer(2L, false), answer(3L, null), answer(4L, true), answer(99L, true)));

        var result = fixture.service.analyzeAttempt(30L, 7L, false);

        assertEquals(66.7, result.getOverallAccuracy(), 0.01);
        assertEquals(7L, result.getUserId());
        assertEquals(5L, result.getCertificateId());
        assertEquals(Map.of("Reading", 50.0, "Writing", 100.0),
                fixture.json.readValue(result.getAccuracyBySkillJson(), new TypeReference<Map<String, Double>>() {}));
        assertEquals(Map.of("Reading", 100.0, "Inference", 0.0, "Writing", 100.0),
                fixture.json.readValue(result.getAccuracyByTopicJson(), new TypeReference<Map<String, Double>>() {}));
        assertEquals(Map.of("EASY", 100.0, "HARD", 0.0, "MEDIUM", 100.0),
                fixture.json.readValue(result.getAccuracyByDifficultyJson(), new TypeReference<Map<String, Double>>() {}));
        assertEquals(List.of("Inference"), fixture.json.readValue(result.getWeakTopicsJson(), new TypeReference<List<String>>() {}));
        assertEquals(List.of("Reading", "Writing"), fixture.json.readValue(result.getStrongTopicsJson(), new TypeReference<List<String>>() {}));
        verify(fixture.recommendations).save(any(Recommendation.class));
    }

    @Test
    void missingAndUnsubmittedAttemptsAreRejected() {
        Fixture fixture = new Fixture(0);
        when(fixture.attempts.findById(30L)).thenReturn(Optional.empty());
        assertEquals(ErrorCode.ENTITY_NOT_FOUND,
                assertThrows(CommonException.class, () -> fixture.service.analyzeAttempt(30L, 7L, false))
                        .getErrorCode());

        when(fixture.attempts.findById(31L)).thenReturn(Optional.of(attempt(31L, 7L, null)));
        assertEquals(ErrorCode.BAD_REQUEST,
                assertThrows(CommonException.class, () -> fixture.service.analyzeAttempt(31L, 7L, false))
                        .getErrorCode());
    }

    @Test
    void noGradableAnswersDoesNotInventZeroAccuracyOrRecommendations() throws Exception {
        Fixture fixture = new Fixture(2);
        fixture.examQuestions(List.of(mapping(1L)));
        fixture.questions(List.of(question(1L, 10L, "ESSAY", QuestionDifficulty.EASY)));
        fixture.topics(List.of(topic(10L, "Writing", TopicType.SKILL, null)));
        fixture.answers(List.of(answer(1L, null)));

        var result = fixture.service.analyzeAttempt(30L, 7L, false);

        assertNull(result.getOverallAccuracy());
        assertEquals("{}", result.getAccuracyByTopicJson());
        assertEquals("[]", result.getWeakTopicsJson());
        verify(fixture.recommendations, never()).save(any(Recommendation.class));
    }

    @Test
    void recommendationBoundariesRemainBelow40AndBelow60AndBelow80() {
        assertBoundary(39, RecommendationPriority.HIGH, "below 40%", true);
        assertBoundary(40, RecommendationPriority.HIGH, "below 60%", true);
        assertBoundary(60, RecommendationPriority.MEDIUM, "developing", true);
        assertBoundary(80, null, null, false);
    }

    private void assertBoundary(int correct, RecommendationPriority priority, String reason, boolean expected) {
        Fixture fixture = new Fixture(100 + correct);
        List<ExamQuestion> mappings = new ArrayList<>();
        List<Question> questions = new ArrayList<>();
        List<AttemptAnswer> answers = new ArrayList<>();
        for (int i = 1; i <= 100; i++) {
            mappings.add(mapping((long) i));
            questions.add(question((long) i, 10L, "MULTIPLE_CHOICE", QuestionDifficulty.EASY));
            answers.add(answer((long) i, i <= correct));
        }
        fixture.examQuestions(mappings);
        fixture.questions(questions);
        fixture.topics(List.of(topic(10L, "Reading", TopicType.SKILL, null)));
        fixture.answers(answers);

        fixture.service.analyzeAttempt(30L, 7L, false);

        if (!expected) {
            verify(fixture.recommendations, never()).save(any(Recommendation.class));
        } else {
            var captor = org.mockito.ArgumentCaptor.forClass(Recommendation.class);
            verify(fixture.recommendations).save(captor.capture());
            assertEquals(priority, captor.getValue().getPriority());
            org.junit.jupiter.api.Assertions.assertTrue(captor.getValue().getReason().contains(reason));
        }
    }

    private static ExamAttempt attempt(Long id, Long userId, LocalDateTime submitted) {
        return ExamAttempt.builder().attemptId(id).userId(userId).examId(9L)
                .startedAt(LocalDateTime.now().minusMinutes(5)).submittedAt(submitted).build();
    }

    private static ExamQuestion mapping(Long questionId) {
        return ExamQuestion.builder().examId(9L).questionId(questionId).build();
    }

    private static Question question(Long id, Long topicId, String type, QuestionDifficulty difficulty) {
        return Question.builder().questionId(id).topicId(topicId).questionType(type).difficulty(difficulty).build();
    }

    private static Topic topic(Long id, String name, TopicType type, Long parentId) {
        return Topic.builder().topicId(id).name(name).type(type).parentId(parentId).certificateId(5L).build();
    }

    private static AttemptAnswer answer(Long questionId, Boolean correct) {
        return AttemptAnswer.builder().attemptId(30L).questionId(questionId)
                .userAnswerJson("\"answer\"").isCorrect(correct).build();
    }

    private static class Fixture {
        private final ExamAttemptRepository attempts = mock(ExamAttemptRepository.class);
        private final AttemptAnswerRepository answers = mock(AttemptAnswerRepository.class);
        private final GapAnalysisRepository gaps = mock(GapAnalysisRepository.class);
        private final RecommendationRepository recommendations = mock(RecommendationRepository.class);
        private final ExamQuestionRepository examQuestionRepository = mock(ExamQuestionRepository.class);
        private final QuestionRepository questionRepository = mock(QuestionRepository.class);
        private final TopicRepository topicRepository = mock(TopicRepository.class);
        private final ExamRepository examRepository = mock(ExamRepository.class);
        private final ObjectMapper json = new ObjectMapper();
        private final AnalyticsServiceImpl service;

        private Fixture(int recommendationId) {
            ExamAttempt attempt = attempt(30L, 7L, LocalDateTime.now());
            when(attempts.findById(30L)).thenReturn(Optional.of(attempt));
            when(examRepository.findById(9L)).thenReturn(Optional.of(
                    Exam.builder().examId(9L).certificateId(5L).build()));
            when(gaps.findByAttemptId(30L)).thenReturn(Optional.empty());
            when(gaps.save(any(GapAnalysis.class))).thenAnswer(invocation -> {
                GapAnalysis analysis = invocation.getArgument(0);
                analysis.setGapAnalysisId((long) recommendationId);
                return analysis;
            });
            when(recommendations.existsByGapAnalysisId(anyLong())).thenReturn(false);
            when(recommendations.save(any(Recommendation.class))).thenAnswer(invocation -> invocation.getArgument(0));
            service = new AnalyticsServiceImpl(answers, attempts, gaps, recommendations,
                    mock(LearningProgressRepository.class), json, examRepository, examQuestionRepository,
                    questionRepository, topicRepository, mock(UserMapper.class));
        }

        private void examQuestions(List<ExamQuestion> mappings) {
            when(examQuestionRepository.findByExamId(9L)).thenReturn(mappings);
        }

        private void questions(List<Question> questions) {
            when(questionRepository.findAllById(any())).thenReturn(questions);
        }

        private void topics(List<Topic> topics) {
            when(topicRepository.findByCertificateId(5L)).thenReturn(topics);
        }

        private void answers(List<AttemptAnswer> answers) {
            when(this.answers.findByAttemptId(30L)).thenReturn(answers);
        }
    }
}
