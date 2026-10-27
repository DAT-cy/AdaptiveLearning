package adaptivelearning.module.users.dto.response;

import java.time.LocalDateTime;

public record EmailVerificationResponse(
        String email,
        LocalDateTime expiresAt,
        LocalDateTime resendAvailableAt,
        boolean verified) {}
