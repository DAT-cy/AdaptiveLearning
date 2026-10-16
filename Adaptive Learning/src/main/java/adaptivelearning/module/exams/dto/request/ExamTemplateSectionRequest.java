package adaptivelearning.module.exams.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamTemplateSectionRequest {

    @NotBlank
    @Size(max = 120)
    private String name;

    @NotNull
    private Long topicId;

    @NotBlank
    @Size(max = 50)
    private String questionType;

    @NotNull
    @Min(1)
    @Max(200)
    private Integer questionCount;

    @DecimalMin("0.0")
    @Builder.Default
    private double pointsPerQuestion = 1.0;
}
