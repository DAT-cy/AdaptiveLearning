package adaptivelearning.module.billing.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PaymentCheckoutRequest {
    @NotBlank
    @Pattern(regexp = "(?i)stripe|paypal")
    private String provider;
}
