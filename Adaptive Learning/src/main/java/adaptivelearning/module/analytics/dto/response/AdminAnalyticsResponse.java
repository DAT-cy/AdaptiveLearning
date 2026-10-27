package adaptivelearning.module.analytics.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record AdminAnalyticsResponse(
        String period,
        Long certificateId,
        LocalDate from,
        LocalDate to,
        long startedAttempts,
        long completedAttempts,
        Double completionRate,
        Double completionRateChange,
        Double averageScorePercent,
        Double averageScoreChange,
        Double passRate,
        long activeLearners,
        List<TrendPoint> trend,
        List<LearnerRow> learnersToSupport) {

    public record TrendPoint(
            LocalDate date,
            String label,
            long attempts,
            Double averageScorePercent,
            String annotation) {}

    public record LearnerRow(
            Long userId,
            String username,
            long attempts,
            Double averageScorePercent,
            Double latestScorePercent,
            LocalDateTime lastAttemptAt) {}
}
