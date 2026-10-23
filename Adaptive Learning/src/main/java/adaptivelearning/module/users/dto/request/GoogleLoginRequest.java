package adaptivelearning.module.users.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class GoogleLoginRequest {
    @NotBlank(message = "Google ID token is required")
    @Schema(description = "Google ID token returned by Google Identity Services", requiredMode = Schema.RequiredMode.REQUIRED)
    private String idToken;
}
