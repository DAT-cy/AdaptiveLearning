package adaptivelearning.module.analytics.service;

import adaptivelearning.module.analytics.dto.response.GapAnalysisResponse;
import adaptivelearning.module.analytics.dto.response.AdminAnalyticsResponse;
import adaptivelearning.module.analytics.dto.response.ProgressResponse;
import adaptivelearning.module.analytics.dto.response.RecommendationResponse;
import java.util.List;

public interface AnalyticsService {
    AdminAnalyticsResponse getAdminOverview(String period, Long certificateId);
    GapAnalysisResponse analyzeAttempt(Long attemptId, Long userId, boolean admin);
    GapAnalysisResponse getAnalysisByAttempt(Long attemptId, Long userId, boolean admin);
    ProgressResponse getProgressOverview(Long userId, Long certificateId);
    List<RecommendationResponse> getRecommendations(Long userId);
    RecommendationResponse startRecommendation(Long recommendationId, Long userId, boolean admin);
    RecommendationResponse completeRecommendation(Long recommendationId, Long userId, boolean admin);
}
