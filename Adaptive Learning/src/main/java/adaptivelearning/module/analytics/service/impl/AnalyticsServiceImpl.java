package adaptivelearning.module.analytics.service.impl;

import adaptivelearning.config.response.CommonException;
import adaptivelearning.config.response.ErrorCode;
import adaptivelearning.module.analytics.dto.response.GapAnalysisResponse;
import adaptivelearning.module.analytics.dto.response.AdminAnalyticsResponse;
import adaptivelearning.module.analytics.dto.response.ProgressResponse;
import adaptivelearning.module.analytics.dto.response.ProgressResponse.AttemptSummary;
import adaptivelearning.module.analytics.dto.response.RecommendationResponse;
import adaptivelearning.module.analytics.entity.GapAnalysis;
import adaptivelearning.module.analytics.entity.LearningProgress;
import adaptivelearning.module.analytics.entity.Recommendation;
import adaptivelearning.module.analytics.repository.GapAnalysisRepository;
import adaptivelearning.module.analytics.repository.LearningProgressRepository;
import adaptivelearning.module.analytics.repository.RecommendationRepository;
import adaptivelearning.module.analytics.service.AnalyticsService;
import adaptivelearning.module.attempts.entity.AttemptAnswer;
import adaptivelearning.module.attempts.entity.ExamAttempt;
import adaptivelearning.module.attempts.repository.AttemptAnswerRepository;
import adaptivelearning.module.attempts.repository.ExamAttemptRepository;
import adaptivelearning.module.exams.entity.Exam;
import adaptivelearning.module.exams.repository.ExamRepository;
import adaptivelearning.module.topics.entity.Topic;
import adaptivelearning.module.topics.repository.TopicRepository;
import adaptivelearning.module.users.entity.User;
import adaptivelearning.module.users.repository.UserMapper;
import adaptivelearning.shared.enums.RecommendationStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class AnalyticsServiceImpl implements AnalyticsService {

    private static final double THRESHOLD_STRONG = 80.0;
    private static final double THRESHOLD_DEVELOPING = 60.0;
    private static final double THRESHOLD_WEAK = 40.0;

    private final AttemptAnswerRepository answerRepository;
    private final ExamAttemptRepository attemptRepository;
    private final GapAnalysisRepository gapAnalysisRepository;
    private final RecommendationRepository recommendationRepository;
    private final LearningProgressRepository progressRepository;
    private final ObjectMapper objectMapper;
    private final ExamRepository examRepository;
    private final TopicRepository topicRepository;
    private final UserMapper userMapper;

    @Override
    @Transactional(readOnly = true)
    public AdminAnalyticsResponse getAdminOverview(String period, Long certificateId) {
        int periodDays = switch (period) {
            case "7d" -> 7;
            case "30d" -> 30;
            case "90d" -> 90;
            default -> throw new CommonException(ErrorCode.BAD_REQUEST);
        };

        LocalDate today = LocalDate.now();
        LocalDate from = today.minusDays(periodDays - 1L);
        LocalDate previousFrom = from.minusDays(periodDays);
        LocalDateTime until = today.plusDays(1).atStartOfDay();
        List<ExamAttempt> attempts = attemptRepository.findByStartedAtGreaterThanEqualAndStartedAtLessThan(
                previousFrom.atStartOfDay(), until);
        Map<Long, Exam> exams = examRepository.findAllById(
                        attempts.stream().map(ExamAttempt::getExamId).distinct().toList())
                .stream().collect(java.util.stream.Collectors.toMap(Exam::getExamId, exam -> exam));
        Map<Long, String> usernames = userMapper.findAllForAdmin().stream()
                .collect(java.util.stream.Collectors.toMap(User::getUserId, User::getUsername));

        List<ExamAttempt> relevant = attempts.stream()
                .filter(attempt -> {
                    Exam exam = exams.get(attempt.getExamId());
                    return exam != null && (certificateId == null || certificateId.equals(exam.getCertificateId()));
                })
                .toList();
        List<ExamAttempt> current = relevant.stream()
                .filter(attempt -> !attempt.getStartedAt().toLocalDate().isBefore(from))
                .toList();
        List<ExamAttempt> previous = relevant.stream()
                .filter(attempt -> attempt.getStartedAt().toLocalDate().isBefore(from))
                .toList();

        long started = current.size();
        long completed = current.stream().filter(attempt -> attempt.getSubmittedAt() != null).count();
        double currentCompletion = rate(completed, started);
        long previousStarted = previous.size();
        long previousCompleted = previous.stream().filter(attempt -> attempt.getSubmittedAt() != null).count();
        Double completionChange = previousStarted == 0 ? null : roundOne(currentCompletion - rate(previousCompleted, previousStarted));
        Double averageScore = averageScore(current);
        Double previousAverageScore = averageScore(previous);
        Double averageScoreChange = averageScore == null || previousAverageScore == null
                ? null : roundOne(averageScore - previousAverageScore);

        List<ExamAttempt> graded = current.stream()
                .filter(attempt -> attempt.getSubmittedAt() != null && scorePercent(attempt) != null)
                .toList();
        long passed = graded.stream().filter(attempt -> {
            Exam exam = exams.get(attempt.getExamId());
            return exam != null && attempt.getScore() >= exam.getPassingScore();
        }).count();
        Double passRate = graded.isEmpty() ? null : roundOne(rate(passed, graded.size()));
        long activeLearners = current.stream().map(ExamAttempt::getUserId).distinct().count();

        List<AdminAnalyticsResponse.TrendPoint> trend = buildTrend(current, from, periodDays);
        Map<Long, List<ExamAttempt>> attemptsByUser = graded.stream()
                .collect(java.util.stream.Collectors.groupingBy(ExamAttempt::getUserId));
        List<AdminAnalyticsResponse.LearnerRow> learners = attemptsByUser.entrySet().stream()
                .map(entry -> {
                    List<ExamAttempt> learnerAttempts = entry.getValue();
                    ExamAttempt latest = learnerAttempts.stream()
                            .max(Comparator.comparing(ExamAttempt::getSubmittedAt)).orElseThrow();
                    return new AdminAnalyticsResponse.LearnerRow(
                            entry.getKey(),
                            usernames.getOrDefault(entry.getKey(), "Học viên #" + entry.getKey()),
                            learnerAttempts.size(),
                            averageScore(learnerAttempts),
                            scorePercent(latest),
                            latest.getSubmittedAt());
                })
                .sorted(Comparator.comparing(AdminAnalyticsResponse.LearnerRow::averageScorePercent))
                .limit(6)
                .toList();

        return new AdminAnalyticsResponse(period, certificateId, from, today, started, completed,
                started == 0 ? null : roundOne(currentCompletion), completionChange,
                averageScore, averageScoreChange, passRate, activeLearners, trend, learners);
    }

    private List<AdminAnalyticsResponse.TrendPoint> buildTrend(List<ExamAttempt> attempts, LocalDate from, int days) {
        int bucketDays = days == 7 ? 1 : 7;
        int bucketCount = (int) Math.ceil(days / (double) bucketDays);
        List<AdminAnalyticsResponse.TrendPoint> points = new ArrayList<>();
        long maxCount = 0;
        int maxIndex = -1;
        for (int index = 0; index < bucketCount; index++) {
            LocalDate bucketFrom = from.plusDays((long) index * bucketDays);
            LocalDate bucketUntil = bucketFrom.plusDays(bucketDays);
            List<ExamAttempt> bucket = attempts.stream()
                    .filter(attempt -> {
                        LocalDate date = attempt.getStartedAt().toLocalDate();
                        return !date.isBefore(bucketFrom) && date.isBefore(bucketUntil);
                    })
                    .toList();
            if (bucket.size() > maxCount) {
                maxCount = bucket.size();
                maxIndex = index;
            }
            points.add(new AdminAnalyticsResponse.TrendPoint(
                    bucketFrom,
                    bucketDays == 1 ? bucketFrom.format(java.time.format.DateTimeFormatter.ofPattern("d/M"))
                            : bucketFrom.format(java.time.format.DateTimeFormatter.ofPattern("d MMM")),
                    bucket.size(),
                    averageScore(bucket),
                    null));
        }
        double meanCount = attempts.size() / (double) Math.max(bucketCount, 1);
        if (maxIndex >= 0 && maxCount >= 3 && maxCount >= meanCount * 1.7) {
            AdminAnalyticsResponse.TrendPoint peak = points.get(maxIndex);
            points.set(maxIndex, new AdminAnalyticsResponse.TrendPoint(
                    peak.date(), peak.label(), peak.attempts(), peak.averageScorePercent(), "Nhiều lượt thi"));
        }
        return points;
    }

    private Double averageScore(List<ExamAttempt> attempts) {
        var average = attempts.stream().map(this::scorePercent).filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue).average();
        return average.isPresent() ? roundOne(average.getAsDouble()) : null;
    }

    private Double scorePercent(ExamAttempt attempt) {
        if (attempt.getScore() == null || attempt.getMaxScore() == null || attempt.getMaxScore() <= 0) return null;
        return roundOne(attempt.getScore() / attempt.getMaxScore() * 100.0);
    }

    private double rate(long numerator, long denominator) {
        return denominator == 0 ? 0 : numerator * 100.0 / denominator;
    }

    private static double roundOne(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    @Override
    public GapAnalysisResponse analyzeAttempt(Long attemptId) {
        ExamAttempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new CommonException(ErrorCode.ENTITY_NOT_FOUND));

        List<AttemptAnswer> answers = answerRepository.findByAttemptId(attemptId);

        long correctCount = answers.stream()
                .filter(a -> Boolean.TRUE.equals(a.getIsCorrect()))
                .count();
        double overallAccuracy = answers.isEmpty()
                ? 0.0
                : (correctCount * 100.0 / answers.size());

        GapAnalysis analysis = gapAnalysisRepository.findByAttemptId(attemptId)
                .orElseGet(GapAnalysis::new);
        analysis.setAttemptId(attemptId);
        analysis.setUserId(attempt.getUserId());
        analysis.setCertificateId(0L);
        analysis.setOverallAccuracy(overallAccuracy);
        analysis.setGeneratedAt(LocalDateTime.now());
        analysis.setAccuracyBySkillJson("{}");
        analysis.setAccuracyByTopicJson("{}");
        analysis.setAccuracyByDifficultyJson("{}");
        analysis.setWeakTopicsJson(overallAccuracy < THRESHOLD_DEVELOPING ? "[\"overall\"]" : "[]");
        analysis.setStrongTopicsJson(overallAccuracy > THRESHOLD_STRONG ? "[\"overall\"]" : "[]");

        GapAnalysis saved = gapAnalysisRepository.save(analysis);
        generateRecommendations(attempt.getUserId(), overallAccuracy, saved.getGapAnalysisId());

        log.info("Completed gap analysis for attempt {}, accuracy={}", attemptId, overallAccuracy);
        return toGapAnalysisResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public GapAnalysisResponse getAnalysisByAttempt(Long attemptId) {
        return gapAnalysisRepository.findByAttemptId(attemptId)
                .map(this::toGapAnalysisResponse)
                .orElseThrow(() -> new CommonException(ErrorCode.ENTITY_NOT_FOUND));
    }

    @Override
    @Transactional(readOnly = true)
    public ProgressResponse getProgressOverview(Long userId, Long certificateId) {
        LearningProgress progress = progressRepository
                .findByUserIdAndCertificateIdOrderBySnapshotAtDesc(userId, certificateId)
                .stream()
                .findFirst()
                .orElse(null);

        List<ExamAttempt> attempts = attemptRepository.findByUserIdOrderByStartedAtDesc(userId).stream()
                .filter(attempt -> attempt.getSubmittedAt() != null)
                .toList();
        var examsById = examRepository.findAllById(attempts.stream().map(ExamAttempt::getExamId).distinct().toList())
                .stream().collect(java.util.stream.Collectors.toMap(Exam::getExamId, exam -> exam));
        List<ExamAttempt> certificateAttempts = attempts.stream()
                .filter(attempt -> {
                    Exam exam = examsById.get(attempt.getExamId());
                    return exam != null && certificateId.equals(exam.getCertificateId());
                })
                .toList();
        List<ExamAttempt> scoredAttempts = certificateAttempts.stream()
                .filter(attempt -> attempt.getScore() != null && attempt.getMaxScore() != null && attempt.getMaxScore() > 0)
                .toList();
        double average = scoredAttempts.stream()
                .mapToDouble(attempt -> attempt.getScore() / attempt.getMaxScore() * 100.0)
                .average().orElse(progress != null && progress.getAverageScore() != null ? progress.getAverageScore() : 0.0);
        List<AttemptSummary> recentAttempts = certificateAttempts.stream().limit(10)
                .map(attempt -> AttemptSummary.builder()
                        .attemptId(attempt.getAttemptId())
                        .examTitle(examsById.get(attempt.getExamId()).getTitle())
                        .score(attempt.getScore())
                        .bandScore(attempt.getBandScore())
                        .submittedAt(attempt.getSubmittedAt())
                        .build())
                .toList();
        var topicsById = topicRepository.findAllById(certificateAttempts.stream()
                        .map(attempt -> examsById.get(attempt.getExamId()).getTopicId())
                        .filter(java.util.Objects::nonNull).distinct().toList())
                .stream().collect(java.util.stream.Collectors.toMap(Topic::getTopicId, topic -> topic));
        var masteryByTopic = scoredAttempts.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        attempt -> examsById.get(attempt.getExamId()).getTopicId(),
                        java.util.stream.Collectors.averagingDouble(
                                attempt -> attempt.getScore() / attempt.getMaxScore() * 100.0)));
        var masteryBySkill = new java.util.LinkedHashMap<String, Double>();
        masteryByTopic.forEach((topicId, score) -> {
            Topic topic = topicsById.get(topicId);
            if (topic != null) masteryBySkill.put(topic.getName(), Math.round(score * 10.0) / 10.0);
        });
        List<java.util.Map<String, Object>> scoreTrend = scoredAttempts.stream()
                .limit(10)
                .sorted(java.util.Comparator.comparing(ExamAttempt::getSubmittedAt))
                .map(attempt -> java.util.Map.<String, Object>of(
                        "attemptId", attempt.getAttemptId(),
                        "scorePercent", Math.round(attempt.getScore() / attempt.getMaxScore() * 1000.0) / 10.0,
                        "submittedAt", attempt.getSubmittedAt().toString()))
                .toList();

        return ProgressResponse.builder()
                .userId(userId)
                .certificateId(certificateId)
                .averageScore(scoredAttempts.isEmpty() && progress == null ? null : average)
                .totalAttempts(certificateAttempts.size())
                .masteryBySkillJson(masteryBySkill.isEmpty() && progress != null
                        ? progress.getMasteryBySkillJson() : toJson(masteryBySkill))
                .scoreTrendJson(scoredAttempts.isEmpty() && progress != null
                        ? progress.getScoreTrendJson() : toJson(scoreTrend))
                .recentAttempts(recentAttempts)
                .build();
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            log.warn("Could not serialize learning progress", exception);
            return "{}";
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<RecommendationResponse> getRecommendations(Long userId) {
        return recommendationRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toRecommendationResponse)
                .toList();
    }

    @Override
    public RecommendationResponse startRecommendation(Long recommendationId) {
        return updateRecommendationStatus(recommendationId, RecommendationStatus.STARTED);
    }

    @Override
    public RecommendationResponse completeRecommendation(Long recommendationId) {
        return updateRecommendationStatus(recommendationId, RecommendationStatus.COMPLETED);
    }

    private void generateRecommendations(Long userId, double accuracy, Long gapAnalysisId) {
        if (accuracy < THRESHOLD_WEAK) {
            recommendationRepository.save(Recommendation.builder()
                    .userId(userId)
                    .gapAnalysisId(gapAnalysisId)
                    .priority(adaptivelearning.shared.enums.RecommendationPriority.HIGH)
                    .reason("Performance below 40% indicates critical gaps requiring immediate attention.")
                    .build());
        } else if (accuracy < THRESHOLD_DEVELOPING) {
            recommendationRepository.save(Recommendation.builder()
                    .userId(userId)
                    .gapAnalysisId(gapAnalysisId)
                    .priority(adaptivelearning.shared.enums.RecommendationPriority.HIGH)
                    .reason("Performance below 60% indicates weak areas that need focused practice.")
                    .build());
        } else if (accuracy < THRESHOLD_STRONG) {
            recommendationRepository.save(Recommendation.builder()
                    .userId(userId)
                    .gapAnalysisId(gapAnalysisId)
                    .priority(adaptivelearning.shared.enums.RecommendationPriority.MEDIUM)
                    .reason("Performance is developing. Continue practicing to strengthen weak topics.")
                    .build());
        }
    }

    private RecommendationResponse updateRecommendationStatus(Long id, RecommendationStatus status) {
        Recommendation recommendation = recommendationRepository.findById(id)
                .orElseThrow(() -> new CommonException(ErrorCode.ENTITY_NOT_FOUND));
        recommendation.setStatus(status);
        return toRecommendationResponse(recommendationRepository.save(recommendation));
    }

    private GapAnalysisResponse toGapAnalysisResponse(GapAnalysis a) {
        return GapAnalysisResponse.builder()
                .gapAnalysisId(a.getGapAnalysisId())
                .attemptId(a.getAttemptId())
                .userId(a.getUserId())
                .certificateId(a.getCertificateId())
                .accuracyBySkillJson(a.getAccuracyBySkillJson())
                .accuracyByTopicJson(a.getAccuracyByTopicJson())
                .accuracyByDifficultyJson(a.getAccuracyByDifficultyJson())
                .weakTopicsJson(a.getWeakTopicsJson())
                .strongTopicsJson(a.getStrongTopicsJson())
                .overallAccuracy(a.getOverallAccuracy())
                .generatedAt(a.getGeneratedAt())
                .build();
    }

    private RecommendationResponse toRecommendationResponse(Recommendation r) {
        return RecommendationResponse.builder()
                .recommendationId(r.getRecommendationId())
                .userId(r.getUserId())
                .gapAnalysisId(r.getGapAnalysisId())
                .practicePackageId(r.getPracticePackageId())
                .reason(r.getReason())
                .priority(r.getPriority())
                .status(r.getStatus())
                .build();
    }
}
