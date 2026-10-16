package adaptivelearning.module.exams.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Data;

@Data
public class ExamTemplateRequest {

    @NotNull
    private Long certificateId;

    @NotNull
    private Long topicId;

    @NotBlank
    @Size(max = 200)
    private String title;

    private String description;

    @Min(1)
    @Max(600)
    private int durationMinutes;

    @NotNull
    @jakarta.validation.constraints.DecimalMin("0.0")
    private Double targetScoreMin;

    @NotNull
    @jakarta.validation.constraints.DecimalMin("0.0")
    private Double targetScoreMax;

    @jakarta.validation.constraints.DecimalMin("0.0")
    private double passingScore;

    @NotEmpty
    @Valid
    private List<ExamTemplateSectionRequest> sections;
}
