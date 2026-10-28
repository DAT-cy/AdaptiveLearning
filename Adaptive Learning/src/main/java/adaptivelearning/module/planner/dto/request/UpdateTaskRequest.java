package adaptivelearning.module.planner.dto.request;
import adaptivelearning.shared.enums.PlanTaskStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateTaskRequest {
    @NotNull
    private PlanTaskStatus status;

    @Size(max = 1000)
    private String notes;
}
