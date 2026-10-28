package adaptivelearning.module.billing.service;

import adaptivelearning.config.response.CommonException;
import adaptivelearning.config.response.ErrorCode;
import adaptivelearning.module.billing.dto.CheckoutResponse;
import adaptivelearning.module.billing.dto.NormalizedPaymentEvent;
import adaptivelearning.module.billing.dto.PaymentStatusResponse;
import adaptivelearning.module.billing.entity.Payment;
import adaptivelearning.module.billing.entity.PaymentWebhookEvent;
import adaptivelearning.module.billing.payment.PaymentStrategy;
import adaptivelearning.module.billing.repository.PaymentRepository;
import adaptivelearning.module.billing.repository.PaymentWebhookEventRepository;
import adaptivelearning.module.notifications.service.NotificationService;
import adaptivelearning.module.users.entity.User;
import adaptivelearning.module.users.repository.UserMapper;
import adaptivelearning.shared.enums.PaymentEventStatus;
import adaptivelearning.shared.enums.PaymentProvider;
import adaptivelearning.shared.enums.PaymentStatus;
import adaptivelearning.shared.enums.PlanType;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentService {
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(PaymentService.class);
    private final PaymentRepository paymentRepository;
    private final PaymentWebhookEventRepository eventRepository;
    private final UserMapper userMapper;
    private final BillingProperties billingProperties;
    private final PaymentStrategyRegistry strategyRegistry;
    private final EntitlementService entitlementService;
    private final NotificationService notificationService;

    public CheckoutResponse checkout(Long userId, String providerValue) {
        User user = requireUser(userId);
        if (user.getPlan() == PlanType.PREMIUM || user.getRole() == adaptivelearning.module.users.entity.Role.ROLE_ADMIN) {
            throw new CommonException(ErrorCode.PAYMENT_ALREADY_PREMIUM);
        }
        BigDecimal price = billingProperties.premiumPrice();
        if (price == null) throw new CommonException(ErrorCode.PAYMENT_CONFIGURATION_MISSING);
        String currency = billingProperties.premiumCurrency();
        if (!"VND".equals(currency) || price.stripTrailingZeros().scale() > 0) {
            throw new CommonException(ErrorCode.PAYMENT_CONFIGURATION_MISSING);
        }

        PaymentProvider provider;
        try {
            provider = PaymentProvider.from(providerValue);
        } catch (RuntimeException exception) {
            throw new CommonException(ErrorCode.BAD_REQUEST);
        }
        PaymentStrategy strategy = strategyRegistry.get(provider);
        Payment payment = new Payment();
        payment.setUserId(userId);
        payment.setProvider(provider);
        payment.setAmount(price);
        payment.setCurrency(currency);
        payment.setStatus(PaymentStatus.PENDING);
        Payment saved = paymentRepository.saveAndFlush(payment);

        try {
            var checkout = strategy.createCheckout(saved, user);
            saved.setProviderPaymentId(checkout.providerPaymentId());
            saved.setCheckoutUrl(checkout.redirectUrl());
            saved.setRawPayload(checkout.rawPayload());
            paymentRepository.save(saved);
            return new CheckoutResponse(saved.getPaymentId(), provider.name(), checkout.redirectUrl());
        } catch (CommonException exception) {
            saved.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(saved);
            throw exception;
        } catch (RuntimeException exception) {
            saved.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(saved);
            LOG.warn("Checkout creation failed for payment {} through {} ({}): {}", saved.getPaymentId(), provider,
                    exception.getClass().getSimpleName(), exception.getMessage());
            throw new CommonException(ErrorCode.PAYMENT_PROVIDER_UNAVAILABLE);
        }
    }

    @Transactional(readOnly = true)
    public PaymentStatusResponse getPayment(Long paymentId, Long userId) {
        Payment payment = paymentRepository.findByPaymentIdAndUserId(paymentId, userId)
                .orElseThrow(() -> new CommonException(ErrorCode.PAYMENT_NOT_FOUND));
        return toStatus(payment);
    }

    @Transactional(readOnly = true)
    public java.util.List<PaymentStatusResponse> getRecentPayments(Long userId) {
        return paymentRepository.findTop10ByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toStatus).toList();
    }

    @Transactional
    public void handleWebhook(PaymentProvider provider, byte[] rawBody,
                              org.springframework.http.HttpHeaders headers) {
        PaymentStrategy strategy = strategyRegistry.get(provider);
        NormalizedPaymentEvent event = strategy.verifyAndParseWebhook(rawBody, headers);
        LOG.info("Verified payment webhook provider={} event={} status={}",
                event.provider(), event.eventId(), event.status());
        if (event.provider() != provider || event.eventId() == null || event.eventId().isBlank()) {
            LOG.warn("Rejected payment webhook because provider or event ID was invalid.");
            throw new CommonException(ErrorCode.PAYMENT_EVENT_INVALID);
        }
        if (event.status() == PaymentEventStatus.IGNORED) return;
        handleVerifiedEvent(event);
    }

    @Transactional
    public Long confirmPayPalReturn(String orderId) {
        Payment payment = paymentRepository.findByProviderAndProviderPaymentId(PaymentProvider.PAYPAL, orderId)
                .orElseThrow(() -> new CommonException(ErrorCode.PAYMENT_NOT_FOUND));
        if (payment.getProvider() != PaymentProvider.PAYPAL || payment.getStatus() != PaymentStatus.PENDING) {
            throw new CommonException(ErrorCode.PAYMENT_RETURN_INVALID);
        }
        NormalizedPaymentEvent event = strategyRegistry.get(PaymentProvider.PAYPAL).confirmReturn(payment, orderId);
        if (event.status() == PaymentEventStatus.IGNORED) return payment.getPaymentId();
        handleVerifiedEvent(event);
        return payment.getPaymentId();
    }

    public String paymentReturnUrl(Long paymentId) {
        return billingProperties.appUrl() + "/payment/success?paymentId=" + paymentId;
    }

    public String paymentCancelUrl() {
        return billingProperties.appUrl() + "/payment/cancel";
    }

    @Transactional(readOnly = true)
    public Long getPayPalPaymentId(String orderId) {
        return paymentRepository.findByProviderAndProviderPaymentId(PaymentProvider.PAYPAL, orderId)
                .map(Payment::getPaymentId)
                .orElseThrow(() -> new CommonException(ErrorCode.PAYMENT_NOT_FOUND));
    }

    @Transactional
    public void handleVerifiedEvent(NormalizedPaymentEvent event) {
        if (eventRepository.existsByProviderAndEventId(event.provider(), event.eventId())) return;

        Payment payment = findPayment(event);
        validateRemotePayment(payment, event);
        PaymentWebhookEvent storedEvent = new PaymentWebhookEvent();
        storedEvent.setProvider(event.provider());
        storedEvent.setEventId(event.eventId());
        storedEvent.setRawPayload(event.rawPayload());
        storedEvent.setProcessedAt(java.time.LocalDateTime.now());
        eventRepository.saveAndFlush(storedEvent);

        payment.setRawPayload(event.rawPayload());
        if (event.providerTransactionId() != null) payment.setProviderTransactionId(event.providerTransactionId());
        switch (event.status()) {
            case SUCCEEDED -> {
                if (payment.getStatus() != PaymentStatus.REFUNDED) {
                    boolean firstSuccess = payment.getStatus() != PaymentStatus.SUCCEEDED;
                    payment.setStatus(PaymentStatus.SUCCEEDED);
                    if (firstSuccess) entitlementService.grantPremium(payment.getUserId());
                    paymentRepository.save(payment);
                    if (firstSuccess) notificationService.createPaymentSucceeded(payment);
                }
            }
            case FAILED -> {
                if (payment.getStatus() == PaymentStatus.PENDING) payment.setStatus(PaymentStatus.FAILED);
            }
            case REFUNDED -> {
                payment.setStatus(PaymentStatus.REFUNDED);
                entitlementService.revokePremium(payment.getUserId());
            }
            case IGNORED -> { }
        }
        paymentRepository.save(payment);
        LOG.info("Processed payment status {} for internal payment {} via {}",
                event.status(), payment.getPaymentId(), payment.getProvider());
    }

    private Payment findPayment(NormalizedPaymentEvent event) {
        if (event.paymentId() != null) {
            Payment payment = paymentRepository.findById(event.paymentId())
                    .orElseThrow(() -> new CommonException(ErrorCode.PAYMENT_NOT_FOUND));
            if (payment.getProvider() != event.provider()) throw new CommonException(ErrorCode.PAYMENT_EVENT_INVALID);
            return payment;
        }
        if (event.providerPaymentId() != null) {
            Payment payment = paymentRepository.findByProviderAndProviderPaymentId(event.provider(), event.providerPaymentId())
                    .orElse(null);
            if (payment != null) return payment;
        }
        if (event.providerTransactionId() != null) {
            return paymentRepository.findByProviderAndProviderTransactionId(event.provider(), event.providerTransactionId())
                    .orElseThrow(() -> new CommonException(ErrorCode.PAYMENT_NOT_FOUND));
        }
        throw new CommonException(ErrorCode.PAYMENT_EVENT_INVALID);
    }

    private void validateRemotePayment(Payment payment, NormalizedPaymentEvent event) {
        if (event.status() == PaymentEventStatus.SUCCEEDED) {
            if (event.amount() == null || payment.getAmount().compareTo(event.amount()) != 0
                    || event.currency() == null
                    || !payment.getCurrency().equalsIgnoreCase(event.currency())) {
                LOG.warn("Rejected verified {} event {} because amount/currency did not match payment {}.",
                        event.provider(), event.eventId(), payment.getPaymentId());
                throw new CommonException(ErrorCode.PAYMENT_EVENT_INVALID);
            }
        }
    }

    private User requireUser(Long userId) {
        return userMapper.findById(userId).orElseThrow(() -> new CommonException(ErrorCode.ENTITY_NOT_FOUND));
    }

    private PaymentStatusResponse toStatus(Payment payment) {
        return new PaymentStatusResponse(payment.getPaymentId(), payment.getProvider(), payment.getAmount(),
                payment.getCurrency(), payment.getStatus(), payment.getCreatedAt());
    }
}
