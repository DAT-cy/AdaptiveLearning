package adaptivelearning.module.billing.dto;

public record CheckoutResponse(Long paymentId, String provider, String redirectUrl) {}
