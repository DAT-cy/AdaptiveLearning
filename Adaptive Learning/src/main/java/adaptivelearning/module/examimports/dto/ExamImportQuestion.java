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
    private Long topicId;
    private String skill;
    private String sourceQuestionNumber;
    private Integer sourcePageNumber;
    private Integer sourceSectionOrder;
    private String sourceSectionTitle;
    private String sourceSectionQuestionStart;
    private String sourceSectionQuestionEnd;
    private String sectionInstructions;
    private String stimulusContent;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ExamImportOption {
        private String label;
        private String text;
    }
}
