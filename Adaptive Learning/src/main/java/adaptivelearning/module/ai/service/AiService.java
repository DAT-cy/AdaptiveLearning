package adaptivelearning.module.ai.service;

import adaptivelearning.module.ai.dto.request.AiGradeRequest;
import adaptivelearning.module.ai.dto.request.RubricRequest;
import adaptivelearning.module.ai.dto.response.AiEvaluationResponse;
import adaptivelearning.module.ai.dto.response.AiJobResponse;
import adaptivelearning.module.ai.dto.response.RubricResponse;
import java.util.List;

public interface AiService {
    AiEvaluationResponse gradeWriting(AiGradeRequest request);
    AiEvaluationResponse getEvaluation(Long evaluationId, Long userId, boolean admin);
    List<AiEvaluationResponse> getEvaluationByAttempt(Long attemptId, Long userId, boolean admin);
    RubricResponse createRubric(RubricRequest request);
    RubricResponse updateRubric(Long rubricId, RubricRequest request);
    List<RubricResponse> getRubricsByCertificate(Long certificateId);
    AiJobResponse submitGradeJob(AiGradeRequest request, Long userId, boolean admin);
    void processPendingJobs();
    AiJobResponse retryJob(Long jobId);
    List<AiJobResponse> getJobs();
}
