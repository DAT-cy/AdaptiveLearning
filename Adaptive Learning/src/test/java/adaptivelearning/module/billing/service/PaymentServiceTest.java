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
import adaptivelearning.module.billing.dto.CheckoutResult;
import adaptivelearning.module.billing.entity.Payment;
import adaptivelearning.module.billing.entity.PaymentWebhookEvent;
import adaptivelearning.module.billing.payment.PaymentStrategy;
import adaptivelearning.module.billing.repository.PaymentRepository;
import adaptivelearning.module.billing.repository.PaymentWebhookEventRepository;
import adaptivelearning.module.notifications.service.NotificationService;
import adaptivelearning.module.users.entity.Role;
import adaptivelearning.module.users.entity.User;
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
    private NotificationService notifications;
    private PaymentStrategy strategy;
    private PaymentService service;
    private Payment payment;
    private NormalizedPaymentEvent success;
    private UserMapper users;

    @BeforeEach
    void setUp() {
        payments = mock(PaymentRepository.class);
        events = mock(PaymentWebhookEventRepository.class);
        users = mock(UserMapper.class);
        BillingProperties properties = new BillingProperties();
        ReflectionTestUtils.setField(properties, "premiumPriceValue", "120000");
        ReflectionTestUtils.setField(properties, "premiumCurrencyValue", "VND");
        ReflectionTestUtils.setField(properties, "freeExamLimitValue", 5);
        PaymentStrategyRegistry registry = mock(PaymentStrategyRegistry.class);
        strategy = mock(PaymentStrategy.class);
        when(registry.get(PaymentProvider.STRIPE)).thenReturn(strategy);
        when(strategy.provider()).thenReturn(PaymentProvider.STRIPE);
        entitlements = mock(EntitlementService.class);
        notifications = mock(NotificationService.class);
        service = new PaymentService(payments, events, users, properties, registry, entitlements, notifications);

        payment = new Payment();
        payment.setPaymentId(31L);
        payment.setUserId(7L);
        payment.setProvider(PaymentProvider.STRIPE);
        payment.setProviderPaymentId("cs_test_31");
        payment.setAmount(new BigDecimal("120000"));
        payment.setCurrency("VND");
        payment.setStatus(PaymentStatus.PENDING);
        success = new NormalizedPaymentEvent(PaymentProvider.STRIPE, "evt_success_31", "cs_test_31",
                "pi_31", 31L, PaymentEventStatus.SUCCEEDED, new BigDecimal("120000"), "VND", "{}");
        when(payments.findById(31L)).thenReturn(Optional.of(payment));
        when(events.existsByProviderAndEventId(any(), any())).thenReturn(false, true);
        when(events.saveAndFlush(any(PaymentWebhookEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void checkoutPersistsIntegerVndPendingPayment() {
        User user = new User();
        user.setUserId(7L);
        user.setRole(Role.ROLE_USER);
        user.setPlan(adaptivelearning.shared.enums.PlanType.FREE);
        when(users.findById(7L)).thenReturn(Optional.of(user));
        when(payments.saveAndFlush(any(Payment.class))).thenAnswer(invocation -> {
            Payment saved = invocation.getArgument(0);
            saved.setPaymentId(32L);
            return saved;
        });
        when(strategy.createCheckout(any(Payment.class), org.mockito.ArgumentMatchers.eq(user)))
                .thenReturn(new CheckoutResult("cs_test_32", "https://checkout.test/32", "{}"));

        var result = service.checkout(7L, "STRIPE");

        assertEquals(32L, result.paymentId());
        var captor = org.mockito.ArgumentCaptor.forClass(Payment.class);
        verify(payments).saveAndFlush(captor.capture());
        assertEquals(new BigDecimal("120000"), captor.getValue().getAmount());
        assertEquals("VND", captor.getValue().getCurrency());
        assertEquals(PaymentStatus.PENDING, captor.getValue().getStatus());
    }

    @Test
    void verifiedSuccessfulEventGrantsPremiumExactlyOnce() {
        when(strategy.verifyAndParseWebhook(any(), any())).thenReturn(success);

        service.handleWebhook(PaymentProvider.STRIPE, "{}".getBytes(), new HttpHeaders());
        service.handleWebhook(PaymentProvider.STRIPE, "{}".getBytes(), new HttpHeaders());

        assertEquals(PaymentStatus.SUCCEEDED, payment.getStatus());
        verify(entitlements).grantPremium(7L);
        verify(notifications).createPaymentSucceeded(payment);
        verify(events).saveAndFlush(any(PaymentWebhookEvent.class));
    }

    @Test
    void laterSuccessEventForAnAlreadyPaidOrderDoesNotRepeatNotification() {
        when(events.existsByProviderAndEventId(any(), any())).thenReturn(false, false);
        when(strategy.verifyAndParseWebhook(any(), any())).thenReturn(success);

        service.handleWebhook(PaymentProvider.STRIPE, "first".getBytes(), new HttpHeaders());
        service.handleWebhook(PaymentProvider.STRIPE, "second".getBytes(), new HttpHeaders());

        verify(entitlements).grantPremium(7L);
        verify(notifications).createPaymentSucceeded(payment);
    }

    @Test
    void amountMismatchNeverGrantsPremium() {
        NormalizedPaymentEvent mismatched = new NormalizedPaymentEvent(PaymentProvider.STRIPE, "evt_bad_31",
                "cs_test_31", "pi_31", 31L, PaymentEventStatus.SUCCEEDED,
                new BigDecimal("1"), "USD", "{}");

        assertThrows(CommonException.class, () -> service.handleVerifiedEvent(mismatched));

        verify(entitlements, never()).grantPremium(7L);
        verify(notifications, never()).createPaymentSucceeded(any(Payment.class));
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
