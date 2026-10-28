package adaptivelearning.module.billing.payment;

import adaptivelearning.module.billing.dto.CheckoutResult;
import adaptivelearning.module.billing.dto.NormalizedPaymentEvent;
import adaptivelearning.module.billing.entity.Payment;
import adaptivelearning.module.billing.service.BillingProperties;
import adaptivelearning.module.users.entity.User;
import adaptivelearning.shared.enums.PaymentEventStatus;
import adaptivelearning.shared.enums.PaymentProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SandboxPaymentStrategy implements PaymentStrategy {
    private final BillingProperties billingProperties;

    @Value("${billing.sandbox.enabled:true}")
    private boolean sandboxEnabled;

    @Override
    public PaymentProvider provider() {
        return PaymentProvider.SANDBOX;
    }

    @Override
    public boolean isConfigured() {
        return sandboxEnabled;
    }

    @Override
    public CheckoutResult createCheckout(Payment payment, User user) {
        String redirectUrl = billingProperties.appUrl() + "/payment/sandbox?paymentId=" + payment.getPaymentId();
        String rawPayload = String.format("{\"paymentId\":%d,\"amount\":%s,\"currency\":\"%s\",\"provider\":\"SANDBOX\"}",
                payment.getPaymentId(), payment.getAmount().toPlainString(), payment.getCurrency());
        return new CheckoutResult("sbx_" + payment.getPaymentId(), redirectUrl, rawPayload);
    }

    @Override
    public NormalizedPaymentEvent verifyAndParseWebhook(byte[] rawBody, HttpHeaders headers) {
        throw new UnsupportedOperationException("Webhook is not required for Sandbox simulation");
    }

    @Override
    public NormalizedPaymentEvent confirmReturn(Payment payment, String tokenOrOrderId) {
        return new NormalizedPaymentEvent(
                PaymentProvider.SANDBOX,
                "sbx_evt_" + payment.getPaymentId() + "_" + System.currentTimeMillis(),
                payment.getProviderPaymentId(),
                "sbx_tx_" + payment.getPaymentId(),
                payment.getPaymentId(),
                PaymentEventStatus.SUCCEEDED,
                payment.getAmount(),
                payment.getCurrency(),
                payment.getRawPayload()
        );
    }
}
