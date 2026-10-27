package adaptivelearning.module.billing.dto;

import adaptivelearning.shared.enums.PaymentEventStatus;
import adaptivelearning.shared.enums.PaymentProvider;
import java.math.BigDecimal;

public record NormalizedPaymentEvent(
        PaymentProvider provider,
        String eventId,
        String providerPaymentId,
        String providerTransactionId,
        Long paymentId,
        PaymentEventStatus status,
        BigDecimal amount,
        String currency,
        String rawPayload) {}
