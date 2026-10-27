package adaptivelearning.module.users.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SignUpRequest {

    @NotBlank
    @NotNull
    @Email(message = "Email must be valid")
    @Size(max = 254)
    private String email;

    @Size(max = 120)
    private String fullName;

    @NotBlank
    @NotNull
    @Size(min = 8, max = 72)
    private String password;
}
