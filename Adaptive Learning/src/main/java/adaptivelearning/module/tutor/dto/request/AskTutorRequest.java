package adaptivelearning.module.tutor.dto.request;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(name = "AskTutorRequest")
public class AskTutorRequest {
    @NotBlank @Size(max = 4000)
    private String content;
    @Size(max = 8000)
    private String contextJson;
}
