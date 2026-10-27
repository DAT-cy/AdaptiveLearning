package adaptivelearning.module.analytics.service;

import adaptivelearning.module.analytics.dto.response.GapAnalysisResponse;
import adaptivelearning.module.analytics.dto.response.AdminAnalyticsResponse;
import adaptivelearning.module.analytics.dto.response.ProgressResponse;
import adaptivelearning.module.analytics.dto.response.RecommendationResponse;
import java.util.List;

public interface AnalyticsService {
    AdminAnalyticsResponse getAdminOverview(String period, Long certificateId);
    GapAnalysisResponse analyzeAttempt(Long attemptId);
    GapAnalysisResponse getAnalysisByAttempt(Long attemptId);
    ProgressResponse getProgressOverview(Long userId, Long certificateId);
    List<RecommendationResponse> getRecommendations(Long userId);
    RecommendationResponse startRecommendation(Long recommendationId);
    RecommendationResponse completeRecommendation(Long recommendationId);
}
