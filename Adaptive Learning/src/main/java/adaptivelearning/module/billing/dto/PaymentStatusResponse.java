package adaptivelearning.module.billing.dto;

import adaptivelearning.shared.enums.PaymentProvider;
import adaptivelearning.shared.enums.PaymentStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentStatusResponse(
        Long paymentId,
        PaymentProvider provider,
        BigDecimal amount,
        String currency,
        PaymentStatus status,
        LocalDateTime createdAt) {}
