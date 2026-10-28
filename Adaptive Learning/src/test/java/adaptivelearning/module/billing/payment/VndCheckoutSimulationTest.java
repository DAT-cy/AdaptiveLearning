package adaptivelearning.module.billing.payment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import adaptivelearning.module.billing.entity.Payment;
import adaptivelearning.module.billing.service.BillingProperties;
import adaptivelearning.module.users.entity.User;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;

@EnabledIfEnvironmentVariable(named = "BILLING_SIM_BASE_URL", matches = ".+")
class VndCheckoutSimulationTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private final BillingProperties properties = new BillingProperties();
    private final Payment payment = new Payment();
    private final User user = new User();
    private String simulationBase;

    @BeforeEach
    void setUp() {
        simulationBase = System.getenv("BILLING_SIM_BASE_URL");
        payment.setPaymentId(73001L);
        payment.setUserId(73L);
        payment.setAmount(new BigDecimal("125000"));
        payment.setCurrency("VND");
        user.setUsername("billing-simulation@example.test");
        ReflectionTestUtils.setField(properties, "appUrlValue", "http://localhost:3000");
    }

    @Test
    void stripeSendsWholeVndAmountWithoutMinorUnitScaling() throws Exception {
        StripePaymentStrategy strategy = new StripePaymentStrategy(WebClient.builder(), mapper, properties);
        ReflectionTestUtils.setField(strategy, "secretKey", "sk_test_simulation_only");
        ReflectionTestUtils.setField(strategy, "webhookSecret", "whsec_simulation_only");
        ReflectionTestUtils.setField(strategy, "apiBaseUrl", simulationBase + "/stripe");

        var checkout = strategy.createCheckout(payment, user);
        JsonNode response = mapper.readTree(checkout.rawPayload());

        assertEquals("vnd", response.path("currency").asText());
        assertEquals("125000", response.path("amount").asText());
    }

    @Test
    void paypalSendsWholeVndAmountWithoutFractionalDigits() throws Exception {
        PayPalPaymentStrategy strategy = new PayPalPaymentStrategy(WebClient.builder(), mapper, properties);
        ReflectionTestUtils.setField(strategy, "clientId", "paypal-client-simulation-only");
        ReflectionTestUtils.setField(strategy, "clientSecret", "paypal-secret-simulation-only");
        ReflectionTestUtils.setField(strategy, "webhookId", "paypal-webhook-simulation-only");
        ReflectionTestUtils.setField(strategy, "configuredBaseUrl", simulationBase + "/paypal");

        var checkout = strategy.createCheckout(payment, user);
        JsonNode response = mapper.readTree(checkout.rawPayload());

        assertEquals("VND", response.path("amount").path("currency_code").asText());
        assertEquals("125000", response.path("amount").path("value").asText());
        assertTrue(response.path("amount").path("value").asText().matches("^[0-9]+$"));
    }
}
