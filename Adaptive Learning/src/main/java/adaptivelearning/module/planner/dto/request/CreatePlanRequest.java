package adaptivelearning.module.planner.dto.request;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import lombok.Data;

@Data
public class CreatePlanRequest {
    @Positive
    private Long certificateId;

    @Min(0)
    private Double targetScore;

    @NotNull
    private LocalDate examDate;

    @NotNull
    @Min(1)
    @Max(80)
    private Integer hoursPerWeek;

    @NotBlank
    @Size(max = 200)
    private String title;
}
