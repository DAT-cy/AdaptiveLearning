package adaptivelearning.module.examimports.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ExamImportQuestion {
    private String questionType;
    private String difficulty;
    private String content;
    private List<ExamImportOption> options;
    private String correctAnswer;
    private String explanation;
    private Double points;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ExamImportOption {
        private String label;
        private String text;
    }
}
