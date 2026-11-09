package adaptivelearning.module.examimports.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ExamImportAnswerKeyResult {
    ExamImportResponse examImport;
    int matchedAnswerCount;
    int totalQuestionCount;
}
