package adaptivelearning.module.billing.dto;

public record CheckoutResult(String providerPaymentId, String redirectUrl, String rawPayload) {}
