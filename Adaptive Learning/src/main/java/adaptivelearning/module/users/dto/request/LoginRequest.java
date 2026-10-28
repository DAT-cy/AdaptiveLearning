package adaptivelearning.module.users.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class LoginRequest {
    @NotBlank(message = "Username is required")
    @Size(max = 254, message = "Username is too long")
    @Schema(title = "username", example = "admin", description = "Username")
    private String username;

    @Schema(title = "password", example = "admin", description = "password")
    @NotBlank(message = "Password is required")
    @Size(max = 72, message = "Password is too long")
    private String password;
}
