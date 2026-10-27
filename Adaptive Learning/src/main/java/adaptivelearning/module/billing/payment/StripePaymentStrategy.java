package adaptivelearning.module.billing.payment;

import adaptivelearning.config.response.CommonException;
import adaptivelearning.config.response.ErrorCode;
import adaptivelearning.module.billing.dto.CheckoutResult;
import adaptivelearning.module.billing.dto.NormalizedPaymentEvent;
import adaptivelearning.module.billing.entity.Payment;
import adaptivelearning.module.billing.service.BillingProperties;
import adaptivelearning.module.users.entity.User;
import adaptivelearning.shared.enums.PaymentEventStatus;
import adaptivelearning.shared.enums.PaymentProvider;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

@Component
@RequiredArgsConstructor
@Slf4j
public class StripePaymentStrategy implements PaymentStrategy {
    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;
    private final BillingProperties billingProperties;

    @Value("${STRIPE_SECRET_KEY:}")
    private String secretKey;

    @Value("${STRIPE_WEBHOOK_SECRET:}")
    private String webhookSecret;

    @Value("${STRIPE_API_BASE_URL:https://api.stripe.com}")
    private String apiBaseUrl;

    @Override public PaymentProvider provider() { return PaymentProvider.STRIPE; }
    @Override public boolean isConfigured() { return hasText(secretKey) && hasText(webhookSecret); }

    @Override
    public CheckoutResult createCheckout(Payment payment, User user) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("mode", "payment");
        form.add("client_reference_id", payment.getPaymentId().toString());
        if (user.getUsername().contains("@")) form.add("customer_email", user.getUsername());
        form.add("line_items[0][quantity]", "1");
        form.add("line_items[0][price_data][currency]", payment.getCurrency().toLowerCase());
        form.add("line_items[0][price_data][unit_amount]", payment.getAmount()
                .movePointRight(2).setScale(0, RoundingMode.UNNECESSARY).toPlainString());
        form.add("line_items[0][price_data][product_data][name]", "Adaptive Learning Premium");
        form.add("metadata[paymentId]", payment.getPaymentId().toString());
        form.add("success_url", billingProperties.appUrl() + "/payment/success?paymentId=" + payment.getPaymentId()
                + "&provider=stripe&session_id={CHECKOUT_SESSION_ID}");
        form.add("cancel_url", billingProperties.appUrl() + "/payment/cancel?paymentId=" + payment.getPaymentId());

        JsonNode response = stripeRequest("/v1/checkout/sessions", form, payment.getPaymentId());
        String id = response.path("id").asText("");
        String url = response.path("url").asText("");
        if (id.isBlank() || url.isBlank()) throw new CommonException(ErrorCode.PAYMENT_PROVIDER_UNAVAILABLE);
        return new CheckoutResult(id, url, response.toString());
    }

    @Override
    public NormalizedPaymentEvent verifyAndParseWebhook(byte[] rawBody, HttpHeaders headers) {
        String signatureHeader = headers.getFirst("Stripe-Signature");
        if (!verifySignature(rawBody, signatureHeader)) throw new CommonException(ErrorCode.PAYMENT_EVENT_INVALID);
        try {
            JsonNode root = objectMapper.readTree(rawBody);
            String eventId = root.path("id").asText("");
            String type = root.path("type").asText("");
            JsonNode object = root.path("data").path("object");
            String sessionId = object.path("id").asText(null);
            String transactionId = object.path("payment_intent").asText(null);
            Long paymentId = parseLong(object.path("metadata").path("paymentId").asText(
                    object.path("client_reference_id").asText(null)));
            PaymentEventStatus status = switch (type) {
                case "checkout.session.completed" -> "paid".equals(object.path("payment_status").asText())
                        ? PaymentEventStatus.SUCCEEDED : PaymentEventStatus.IGNORED;
                case "checkout.session.async_payment_succeeded" -> PaymentEventStatus.SUCCEEDED;
                case "checkout.session.async_payment_failed" -> PaymentEventStatus.FAILED;
                case "charge.refunded" -> {
                    transactionId = object.path("payment_intent").asText(null);
                    boolean fullyRefunded = object.path("refunded").asBoolean(false)
                            || object.path("amount_refunded").asLong(0) >= object.path("amount").asLong(Long.MAX_VALUE);
                    yield fullyRefunded ? PaymentEventStatus.REFUNDED : PaymentEventStatus.IGNORED;
                }
                default -> PaymentEventStatus.IGNORED;
            };
            if (status == PaymentEventStatus.SUCCEEDED && object.has("amount_total")) {
                return event(eventId, sessionId, transactionId, paymentId, status,
                        object.path("amount_total").decimalValue().movePointLeft(2),
                        object.path("currency").asText(null), root.toString());
            }
            return event(eventId, sessionId, transactionId, paymentId, status,
                    null, null, root.toString());
        } catch (CommonException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new CommonException(ErrorCode.PAYMENT_EVENT_INVALID);
        }
    }

    private boolean verifySignature(byte[] rawBody, String header) {
        if (!hasText(header) || !hasText(webhookSecret)) return false;
        String timestamp = null;
        String expected = null;
        for (String pair : header.split(",")) {
            String[] parts = pair.trim().split("=", 2);
            if (parts.length == 2 && "t".equals(parts[0])) timestamp = parts[1];
            if (parts.length == 2 && "v1".equals(parts[0])) expected = parts[1];
        }
        if (timestamp == null || expected == null) return false;
        try {
            long epoch = Long.parseLong(timestamp);
            if (Math.abs(Instant.now().getEpochSecond() - epoch) > Duration.ofMinutes(5).toSeconds()) return false;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] signature = mac.doFinal((timestamp + "." + new String(rawBody, StandardCharsets.UTF_8))
                    .getBytes(StandardCharsets.UTF_8));
            return MessageDigest.isEqual(toHex(signature).getBytes(StandardCharsets.US_ASCII),
                    expected.getBytes(StandardCharsets.US_ASCII));
        } catch (Exception exception) {
            return false;
        }
    }

    private JsonNode stripeRequest(String path, MultiValueMap<String, String> form, Long paymentId) {
        try {
            String baseUrl = apiBaseUrl.endsWith("/") ? apiBaseUrl : apiBaseUrl + "/";
            return webClientBuilder.baseUrl(baseUrl).build().post().uri(path.startsWith("/") ? path.substring(1) : path)
                    .headers(headers -> headers.setBasicAuth(secretKey, ""))
                    .header("Idempotency-Key", "adaptive-payment-" + paymentId)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(BodyInserters.fromFormData(form))
                    .retrieve().bodyToMono(JsonNode.class).block(Duration.ofSeconds(20));
        } catch (Exception exception) {
            log.warn("Stripe checkout API request failed ({}): {}", exception.getClass().getSimpleName(), exception.getMessage());
            throw new CommonException(ErrorCode.PAYMENT_PROVIDER_UNAVAILABLE);
        }
    }

    private NormalizedPaymentEvent event(String id, String providerId, String transactionId, Long paymentId,
                                         PaymentEventStatus status, java.math.BigDecimal amount,
                                         String currency, String raw) {
        return new NormalizedPaymentEvent(PaymentProvider.STRIPE, id, providerId, transactionId,
                paymentId, status, amount, currency, raw);
    }

    private Long parseLong(String value) {
        try { return value == null ? null : Long.valueOf(value); }
        catch (NumberFormatException exception) { return null; }
    }

    private String toHex(byte[] bytes) {
        StringBuilder result = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) result.append(String.format("%02x", value));
        return result.toString();
    }

    private boolean hasText(String value) { return value != null && !value.isBlank(); }
}
