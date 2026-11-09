package adaptivelearning.module.examimports.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ExamImportAnalysisProgress {
    String jobId;
    String status;
    int progress;
    int completedChunks;
    int totalChunks;
    Long importId;
    String message;
    String errorMessage;
}
