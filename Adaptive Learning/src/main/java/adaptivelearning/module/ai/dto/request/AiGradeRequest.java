package adaptivelearning.module.ai.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to grade a writing/speaking answer via AI")
public class AiGradeRequest {

    @NotNull(message = "attemptId is required")
    @Schema(description = "Exam attempt ID", example = "1")
    private Long attemptId;

    @NotNull(message = "questionId is required")
    @Schema(description = "Question ID", example = "10")
    private Long questionId;

    @NotNull(message = "userAnswerText is required")
    @Size(min = 1, max = 20000, message = "userAnswerText must be between 1 and 20000 characters")
    @Schema(description = "User answer text (essay or transcript)", example = "This is my essay...")
    private String userAnswerText;

    @Schema(description = "Optional rubric ID to use for grading", example = "1")
    @Positive(message = "rubricId must be positive")
    private Long rubricId;
}
