package adaptivelearning.module.tutor.dto.request;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(name = "CreateConversationRequest")
public class CreateConversationRequest {
    @Positive
    private Long certificateId;
    @Size(max = 200)
    @Schema(maxLength = 200)
    private String title;
}
