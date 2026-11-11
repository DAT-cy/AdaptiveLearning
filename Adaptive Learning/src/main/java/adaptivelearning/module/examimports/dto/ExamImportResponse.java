package adaptivelearning.module.examimports.dto;

import adaptivelearning.module.examimports.entity.ExamImportStatus;
import java.util.Date;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ExamImportResponse {
    Long importId;
    String title;
    Long certificateId;
    Long topicId;
    boolean canDelete;
    boolean answerGenerated;
    boolean hasAnswers;
    ExamImportStatus status;
    boolean active;
    int questionCount;
    String sourceFileName;
    String sourceFileContentType;
    Long sourceFileSize;
    List<ExamImportResourceResponse> resources;
    List<ExamImportQuestion> questions;
    String sourceContent;
    String answerContent;
    Date createdAt;
}
