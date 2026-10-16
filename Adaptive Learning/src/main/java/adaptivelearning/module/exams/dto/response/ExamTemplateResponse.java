package adaptivelearning.module.exams.dto.response;

import adaptivelearning.module.exams.dto.request.ExamTemplateSectionRequest;
import java.util.Date;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ExamTemplateResponse {
    Long templateId;
    Long certificateId;
    Long topicId;
    String title;
    String description;
    int durationMinutes;
    Double targetScoreMin;
    Double targetScoreMax;
    String targetScoreUnit;
    double passingScore;
    List<ExamTemplateSectionRequest> sections;
    boolean active;
    Date createdAt;
}
