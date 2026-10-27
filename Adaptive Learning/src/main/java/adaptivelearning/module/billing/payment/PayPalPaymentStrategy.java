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
import java.math.BigDecimal;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

@Component
@RequiredArgsConstructor
public class PayPalPaymentStrategy implements PaymentStrategy {
    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;
    private final BillingProperties billingProperties;

    @Value("${PAYPAL_CLIENT_ID:}")
    private String clientId;
    @Value("${PAYPAL_CLIENT_SECRET:}")
    private String clientSecret;
    @Value("${PAYPAL_WEBHOOK_ID:}")
    private String webhookId;
    @Value("${PAYPAL_MODE:sandbox}")
    private String mode;
    @Value("${PAYPAL_API_BASE_URL:}")
    private String configuredBaseUrl;

    @Override public PaymentProvider provider() { return PaymentProvider.PAYPAL; }
    @Override public boolean isConfigured() { return hasText(clientId) && hasText(clientSecret) && hasText(webhookId); }

    @Override
    public CheckoutResult createCheckout(Payment payment, User user) {
        Map<String, Object> amount = Map.of("currency_code", payment.getCurrency(), "value", payment.getAmount().toPlainString());
        Map<String, Object> purchaseUnit = new LinkedHashMap<>();
        purchaseUnit.put("reference_id", payment.getPaymentId().toString());
        purchaseUnit.put("custom_id", payment.getPaymentId().toString());
        purchaseUnit.put("description", "Adaptive Learning Premium");
        purchaseUnit.put("amount", amount);
        Map<String, Object> applicationContext = Map.of(
                "brand_name", "Adaptive Learning",
                "user_action", "PAY_NOW",
                "return_url", billingProperties.apiUrl() + "/api/v1/payments/paypal/return",
                "cancel_url", billingProperties.appUrl() + "/payment/cancel?paymentId=" + payment.getPaymentId());
        JsonNode response = api("v2/checkout/orders")
                .header("PayPal-Request-Id", "adaptive-payment-" + payment.getPaymentId())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("intent", "CAPTURE", "purchase_units", java.util.List.of(purchaseUnit),
                        "application_context", applicationContext))
                .retrieve().bodyToMono(JsonNode.class).block(Duration.ofSeconds(20));
        if (response == null) throw new CommonException(ErrorCode.PAYMENT_PROVIDER_UNAVAILABLE);
        String orderId = response.path("id").asText("");
        String approveUrl = null;
        for (JsonNode link : response.path("links")) {
            if ("approve".equals(link.path("rel").asText())) approveUrl = link.path("href").asText();
        }
        if (orderId.isBlank() || !hasText(approveUrl)) throw new CommonException(ErrorCode.PAYMENT_PROVIDER_UNAVAILABLE);
        return new CheckoutResult(orderId, approveUrl, response.toString());
    }

    @Override
    public NormalizedPaymentEvent verifyAndParseWebhook(byte[] rawBody, HttpHeaders headers) {
        JsonNode root;
        try { root = objectMapper.readTree(rawBody); }
        catch (Exception exception) { throw new CommonException(ErrorCode.PAYMENT_EVENT_INVALID); }
        Map<String, Object> verification = new LinkedHashMap<>();
        verification.put("auth_algo", headers.getFirst("PAYPAL-AUTH-ALGO"));
        verification.put("cert_url", headers.getFirst("PAYPAL-CERT-URL"));
        verification.put("transmission_id", headers.getFirst("PAYPAL-TRANSMISSION-ID"));
        verification.put("transmission_sig", headers.getFirst("PAYPAL-TRANSMISSION-SIG"));
        verification.put("transmission_time", headers.getFirst("PAYPAL-TRANSMISSION-TIME"));
        verification.put("webhook_id", webhookId);
        verification.put("webhook_event", root);
        JsonNode verificationResult = api("v1/notifications/verify-webhook-signature")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(verification)
                .retrieve().bodyToMono(JsonNode.class).block(Duration.ofSeconds(20));
        if (verificationResult == null || !"SUCCESS".equals(verificationResult.path("verification_status").asText())) {
            throw new CommonException(ErrorCode.PAYMENT_EVENT_INVALID);
        }
        JsonNode resource = root.path("resource");
        String eventType = root.path("event_type").asText("");
        String orderId = resource.path("supplementary_data").path("related_ids").path("order_id").asText(null);
        String transactionId = "PAYMENT.CAPTURE.REFUNDED".equals(eventType)
                || "PAYMENT.CAPTURE.REVERSED".equals(eventType)
                ? resource.path("supplementary_data").path("related_ids").path("capture_id").asText(null)
                : resource.path("id").asText(null);
        PaymentEventStatus status = switch (eventType) {
            case "PAYMENT.CAPTURE.COMPLETED" -> "COMPLETED".equals(resource.path("status").asText())
                    ? PaymentEventStatus.SUCCEEDED : PaymentEventStatus.IGNORED;
            case "PAYMENT.CAPTURE.DENIED" -> PaymentEventStatus.FAILED;
            case "PAYMENT.CAPTURE.REFUNDED" -> "REFUNDED".equals(resource.path("status").asText())
                    ? PaymentEventStatus.REFUNDED : PaymentEventStatus.IGNORED;
            case "PAYMENT.CAPTURE.REVERSED" -> PaymentEventStatus.REFUNDED;
            default -> PaymentEventStatus.IGNORED;
        };
        JsonNode amount = resource.path("amount");
        return new NormalizedPaymentEvent(PaymentProvider.PAYPAL, root.path("id").asText(null),
                orderId, transactionId, parseLong(resource.path("custom_id").asText(null)), status,
                amount.isMissingNode() ? null : amount.path("value").decimalValue(),
                amount.path("currency_code").asText(null), root.toString());
    }

    @Override
    public NormalizedPaymentEvent confirmReturn(Payment payment, String providerOrderId) {
        if (!payment.getProviderPaymentId().equals(providerOrderId)) {
            throw new CommonException(ErrorCode.PAYMENT_RETURN_INVALID);
        }
        JsonNode response = api("v2/checkout/orders/" + providerOrderId + "/capture")
                .header("PayPal-Request-Id", "adaptive-capture-" + payment.getPaymentId())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of())
                .retrieve().bodyToMono(JsonNode.class).block(Duration.ofSeconds(20));
        if (response == null) throw new CommonException(ErrorCode.PAYMENT_PROVIDER_UNAVAILABLE);
        JsonNode purchaseUnit = response.path("purchase_units").path(0);
        JsonNode capture = purchaseUnit.path("payments").path("captures").path(0);
        JsonNode amount = capture.path("amount");
        String status = capture.path("status").asText("");
        PaymentEventStatus normalized = "COMPLETED".equals(status)
                ? PaymentEventStatus.SUCCEEDED
                : "PENDING".equals(status) ? PaymentEventStatus.IGNORED : PaymentEventStatus.FAILED;
        return new NormalizedPaymentEvent(PaymentProvider.PAYPAL,
                "return:" + response.path("id").asText(providerOrderId) + ":" + capture.path("id").asText("pending"),
                response.path("id").asText(providerOrderId), capture.path("id").asText(null),
                payment.getPaymentId(), normalized,
                amount.isMissingNode() ? null : new BigDecimal(amount.path("value").asText()),
                amount.path("currency_code").asText(null), response.toString());
    }

    private WebClient.RequestBodySpec api(String path) {
        LinkedMultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        JsonNode tokenResponse = client().post().uri("v1/oauth2/token")
                .headers(headers -> headers.setBasicAuth(clientId, clientSecret))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(form))
                .retrieve().bodyToMono(JsonNode.class).block(Duration.ofSeconds(15));
        String token = tokenResponse == null ? "" : tokenResponse.path("access_token").asText("");
        if (token.isBlank()) throw new CommonException(ErrorCode.PAYMENT_PROVIDER_UNAVAILABLE);
        return client().post().uri(path)
                .headers(headers -> headers.setBearerAuth(token));
    }

    private WebClient client() {
        return webClientBuilder.baseUrl(baseUrl()).build();
    }

    private String baseUrl() {
        String value = hasText(configuredBaseUrl)
                ? configuredBaseUrl.trim()
                : "live".equalsIgnoreCase(mode) ? "https://api-m.paypal.com" : "https://api-m.sandbox.paypal.com";
        return value.endsWith("/") ? value : value + "/";
    }

    private Long parseLong(String value) {
        try { return value == null ? null : Long.valueOf(value); }
        catch (NumberFormatException exception) { return null; }
    }

    private boolean hasText(String value) { return value != null && !value.isBlank(); }
}
