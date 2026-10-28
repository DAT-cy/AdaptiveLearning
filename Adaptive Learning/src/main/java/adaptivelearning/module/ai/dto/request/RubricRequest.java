package adaptivelearning.module.ai.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to create or update a rubric")
public class RubricRequest {

    @NotNull(message = "certificateId is required")
    @Positive(message = "certificateId must be positive")
    @Schema(description = "Certificate ID", example = "1")
    private Long certificateId;

    @NotBlank(message = "skill is required")
    @Size(max = 50)
    @Schema(description = "Skill type, e.g. WRITING, SPEAKING", example = "WRITING")
    private String skill;

    @NotBlank(message = "name is required")
    @Size(max = 200)
    @Schema(description = "Rubric name", example = "IELTS Writing Task 2")
    private String name;

    @Schema(description = "Rubric description")
    @Size(max = 4000)
    private String description;

    @NotNull(message = "criteriaJson is required")
    @Size(max = 20000)
    @Schema(description = "Criteria JSON array", example = "[{\"name\":\"Coherence\",\"maxScore\":9,\"description\":\"...\"}]")
    private String criteriaJson;
}
