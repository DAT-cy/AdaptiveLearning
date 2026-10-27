package adaptivelearning.module.billing.payment;

import adaptivelearning.module.billing.dto.CheckoutResult;
import adaptivelearning.module.billing.dto.NormalizedPaymentEvent;
import adaptivelearning.module.billing.entity.Payment;
import adaptivelearning.shared.enums.PaymentProvider;
import adaptivelearning.module.users.entity.User;
import org.springframework.http.HttpHeaders;

public interface PaymentStrategy {
    PaymentProvider provider();
    boolean isConfigured();
    CheckoutResult createCheckout(Payment payment, User user);
    NormalizedPaymentEvent verifyAndParseWebhook(byte[] rawBody, HttpHeaders headers);

    default NormalizedPaymentEvent confirmReturn(Payment payment, String providerOrderId) {
        throw new UnsupportedOperationException("This payment provider has no return-capture flow.");
    }
}
