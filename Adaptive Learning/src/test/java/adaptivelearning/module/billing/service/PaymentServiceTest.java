package adaptivelearning.module.billing.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import adaptivelearning.config.response.CommonException;
import adaptivelearning.module.billing.dto.NormalizedPaymentEvent;
import adaptivelearning.module.billing.entity.Payment;
import adaptivelearning.module.billing.entity.PaymentWebhookEvent;
import adaptivelearning.module.billing.payment.PaymentStrategy;
import adaptivelearning.module.billing.repository.PaymentRepository;
import adaptivelearning.module.billing.repository.PaymentWebhookEventRepository;
import adaptivelearning.module.users.repository.UserMapper;
import adaptivelearning.shared.enums.PaymentEventStatus;
import adaptivelearning.shared.enums.PaymentProvider;
import adaptivelearning.shared.enums.PaymentStatus;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.test.util.ReflectionTestUtils;

class PaymentServiceTest {
    private PaymentRepository payments;
    private PaymentWebhookEventRepository events;
    private EntitlementService entitlements;
    private PaymentStrategy strategy;
    private PaymentService service;
    private Payment payment;
    private NormalizedPaymentEvent success;

    @BeforeEach
    void setUp() {
        payments = mock(PaymentRepository.class);
        events = mock(PaymentWebhookEventRepository.class);
        UserMapper users = mock(UserMapper.class);
        BillingProperties properties = new BillingProperties();
        ReflectionTestUtils.setField(properties, "premiumPriceValue", "19.99");
        ReflectionTestUtils.setField(properties, "premiumCurrencyValue", "USD");
        ReflectionTestUtils.setField(properties, "freeExamLimitValue", 5);
        PaymentStrategyRegistry registry = mock(PaymentStrategyRegistry.class);
        strategy = mock(PaymentStrategy.class);
        when(registry.get(PaymentProvider.STRIPE)).thenReturn(strategy);
        when(strategy.provider()).thenReturn(PaymentProvider.STRIPE);
        entitlements = mock(EntitlementService.class);
        service = new PaymentService(payments, events, users, properties, registry, entitlements);

        payment = new Payment();
        payment.setPaymentId(31L);
        payment.setUserId(7L);
        payment.setProvider(PaymentProvider.STRIPE);
        payment.setProviderPaymentId("cs_test_31");
        payment.setAmount(new BigDecimal("19.99"));
        payment.setCurrency("USD");
        payment.setStatus(PaymentStatus.PENDING);
        success = new NormalizedPaymentEvent(PaymentProvider.STRIPE, "evt_success_31", "cs_test_31",
                "pi_31", 31L, PaymentEventStatus.SUCCEEDED, new BigDecimal("19.99"), "USD", "{}");
        when(payments.findById(31L)).thenReturn(Optional.of(payment));
        when(events.existsByProviderAndEventId(any(), any())).thenReturn(false, true);
        when(events.saveAndFlush(any(PaymentWebhookEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void verifiedSuccessfulEventGrantsPremiumExactlyOnce() {
        when(strategy.verifyAndParseWebhook(any(), any())).thenReturn(success);

        service.handleWebhook(PaymentProvider.STRIPE, "{}".getBytes(), new HttpHeaders());
        service.handleWebhook(PaymentProvider.STRIPE, "{}".getBytes(), new HttpHeaders());

        assertEquals(PaymentStatus.SUCCEEDED, payment.getStatus());
        verify(entitlements).grantPremium(7L);
        verify(events).saveAndFlush(any(PaymentWebhookEvent.class));
    }

    @Test
    void amountMismatchNeverGrantsPremium() {
        NormalizedPaymentEvent mismatched = new NormalizedPaymentEvent(PaymentProvider.STRIPE, "evt_bad_31",
                "cs_test_31", "pi_31", 31L, PaymentEventStatus.SUCCEEDED,
                new BigDecimal("1.00"), "USD", "{}");

        assertThrows(CommonException.class, () -> service.handleVerifiedEvent(mismatched));

        verify(entitlements, never()).grantPremium(7L);
        verify(events, never()).saveAndFlush(any(PaymentWebhookEvent.class));
    }

    @Test
    void verifiedRefundRevokesPremium() {
        NormalizedPaymentEvent refunded = new NormalizedPaymentEvent(PaymentProvider.STRIPE, "evt_refund_31",
                "ch_31", "pi_31", 31L, PaymentEventStatus.REFUNDED, null, null, "{}");

        service.handleVerifiedEvent(refunded);

        assertEquals(PaymentStatus.REFUNDED, payment.getStatus());
        verify(entitlements).revokePremium(7L);
    }
}
