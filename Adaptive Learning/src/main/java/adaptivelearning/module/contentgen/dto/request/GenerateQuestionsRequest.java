package adaptivelearning.module.contentgen.dto.request;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
@Schema(name = "GenerateQuestionsRequest")
public class GenerateQuestionsRequest {
    @NotNull @Positive
    private Long certificateId;
    @NotNull @Positive
    private Long topicId;
    @NotBlank @Size(max = 50)
    private String questionType;
    @Size(max = 20)
    private String difficulty;
    @Min(1) @Max(20)
    private int count = 5;
    @Size(max = 4000)
    private String instructions;
}
