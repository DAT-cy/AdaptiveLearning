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
    // === Section (parent level: SECTION I, SECTION II, PART I...) ===
    private Integer sourceSectionOrder;
    private String sourceSectionTitle;
    private String sourceSectionQuestionStart;
    private String sourceSectionQuestionEnd;
    private String sectionInstructions;
    private String stimulusContent;
    // === Question Group (child level: "Questions 1-4", "Questions 5-10"...) ===
    private Integer sourceGroupOrder;
    private String sourceGroupTitle;
    private String sourceGroupQuestionStart;
    private String sourceGroupQuestionEnd;
    private String groupInstruction;
    private String groupStimulusContent;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ExamImportOption {
        private String label;
        private String text;
    }
}
