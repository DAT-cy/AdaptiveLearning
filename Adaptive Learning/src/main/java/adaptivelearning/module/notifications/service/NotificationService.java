package adaptivelearning.module.notifications.service;

import adaptivelearning.config.response.CommonException;
import adaptivelearning.config.response.ErrorCode;
import adaptivelearning.module.billing.entity.Payment;
import adaptivelearning.module.notifications.dto.NotificationInboxResponse;
import adaptivelearning.module.notifications.dto.NotificationResponse;
import adaptivelearning.module.notifications.entity.UserNotification;
import adaptivelearning.module.notifications.repository.UserNotificationRepository;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {
    private static final String PAYMENT_SUCCEEDED = "PAYMENT_SUCCEEDED";
    private final UserNotificationRepository repository;
    private final ConcurrentHashMap<Long, Set<SseEmitter>> emitters = new ConcurrentHashMap<>();

    @Transactional(readOnly = true)
    public NotificationInboxResponse getInbox(Long userId) {
        var items = repository.findTop20ByUserIdOrderByCreatedAtDesc(userId)
                .stream().map(this::toResponse).toList();
        return new NotificationInboxResponse(items, repository.countByUserIdAndReadAtIsNull(userId));
    }

    @Transactional
    public NotificationResponse markRead(Long userId, Long notificationId) {
        UserNotification notification = repository.findByNotificationIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new CommonException(ErrorCode.ENTITY_NOT_FOUND));
        if (notification.getReadAt() == null) {
            notification.setReadAt(java.time.LocalDateTime.now());
            notification = repository.save(notification);
        }
        return toResponse(notification);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void createPaymentSucceeded(Payment payment) {
        if (repository.existsByTypeAndPaymentId(PAYMENT_SUCCEEDED, payment.getPaymentId())) return;

        UserNotification notification = new UserNotification();
        notification.setUserId(payment.getUserId());
        notification.setType(PAYMENT_SUCCEEDED);
        notification.setPaymentId(payment.getPaymentId());
        notification.setTitle("Thanh toán thành công");
        notification.setMessage("Đơn hàng #" + payment.getPaymentId() + " đã thanh toán thành công: "
                + formatVnd(payment.getAmount()) + " VND.");
        NotificationResponse response = toResponse(repository.saveAndFlush(notification));

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                publish(notification.getUserId(), response);
            }
        });
    }

    public SseEmitter subscribe(Long userId) {
        SseEmitter emitter = new SseEmitter(0L);
        Set<SseEmitter> userEmitters = emitters.computeIfAbsent(userId, ignored -> new CopyOnWriteArraySet<>());
        userEmitters.add(emitter);
        emitter.onCompletion(() -> remove(userId, emitter));
        emitter.onTimeout(() -> remove(userId, emitter));
        emitter.onError(error -> remove(userId, emitter));
        try {
            emitter.send(SseEmitter.event().name("ready").data("connected"));
        } catch (IOException exception) {
            remove(userId, emitter);
            emitter.completeWithError(exception);
        }
        return emitter;
    }

    @Scheduled(fixedDelay = 25000)
    public void sendHeartbeats() {
        emitters.forEach((userId, userEmitters) -> userEmitters.forEach(emitter -> {
            try {
                emitter.send(SseEmitter.event().name("heartbeat").data("keepalive"));
            } catch (IOException exception) {
                log.debug("Notification stream closed for user {}.", userId);
                remove(userId, emitter);
            }
        }));
    }

    private void publish(Long userId, NotificationResponse notification) {
        Set<SseEmitter> userEmitters = emitters.get(userId);
        if (userEmitters == null) return;
        userEmitters.forEach(emitter -> {
            try {
                emitter.send(SseEmitter.event().name("notification")
                        .id(notification.notificationId().toString()).data(notification));
            } catch (IOException exception) {
                log.debug("Notification stream closed for user {}.", userId);
                remove(userId, emitter);
            }
        });
    }

    private void remove(Long userId, SseEmitter emitter) {
        Set<SseEmitter> userEmitters = emitters.get(userId);
        if (userEmitters != null) {
            userEmitters.remove(emitter);
            if (userEmitters.isEmpty()) emitters.remove(userId, userEmitters);
        }
    }

    private NotificationResponse toResponse(UserNotification notification) {
        return new NotificationResponse(notification.getNotificationId(), notification.getType(),
                notification.getTitle(), notification.getMessage(), notification.getPaymentId(),
                notification.getReadAt() != null, notification.getCreatedAt());
    }

    private String formatVnd(BigDecimal amount) {
        return amount == null ? "0" : amount.toBigInteger().toString();
    }
}
