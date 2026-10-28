package adaptivelearning.module.billing.payment;

import static org.junit.jupiter.api.Assertions.assertEquals;

import adaptivelearning.module.billing.service.BillingProperties;
import adaptivelearning.shared.enums.PaymentEventStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;

class StripeVndWebhookTest {
    @Test
    void parsesStripeVndZeroDecimalAmountWithoutScaling() throws Exception {
        String secret = "whsec-test-only";
        String timestamp = Long.toString(Instant.now().getEpochSecond());
        byte[] raw = ("{\"id\":\"evt_vnd_test\",\"type\":\"checkout.session.completed\","
                + "\"data\":{\"object\":{\"id\":\"cs_vnd_test\",\"payment_status\":\"paid\","
                + "\"amount_total\":125000,\"currency\":\"vnd\",\"metadata\":{\"paymentId\":\"9\"}}}}")
                .getBytes(StandardCharsets.UTF_8);
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String signature = toHex(mac.doFinal((timestamp + "." + new String(raw, StandardCharsets.UTF_8))
                .getBytes(StandardCharsets.UTF_8)));

        StripePaymentStrategy strategy = new StripePaymentStrategy(WebClient.builder(), new ObjectMapper(),
                new BillingProperties());
        ReflectionTestUtils.setField(strategy, "webhookSecret", secret);
        HttpHeaders headers = new HttpHeaders();
        headers.set("Stripe-Signature", "t=" + timestamp + ",v1=" + signature);

        var event = strategy.verifyAndParseWebhook(raw, headers);

        assertEquals(PaymentEventStatus.SUCCEEDED, event.status());
        assertEquals("125000", event.amount().toPlainString());
        assertEquals("vnd", event.currency());
    }

    private String toHex(byte[] bytes) {
        StringBuilder hex = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) hex.append(String.format("%02x", value));
        return hex.toString();
    }
}
