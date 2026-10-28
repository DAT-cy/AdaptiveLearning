package adaptivelearning.module.notifications.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import adaptivelearning.config.response.CommonException;
import adaptivelearning.module.billing.entity.Payment;
import adaptivelearning.module.notifications.entity.UserNotification;
import adaptivelearning.module.notifications.repository.UserNotificationRepository;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronizationManager;

class NotificationServiceTest {
    private UserNotificationRepository repository;
    private NotificationService service;

    @BeforeEach
    void setUp() {
        repository = mock(UserNotificationRepository.class);
        service = new NotificationService(repository);
    }

    @Test
    void createsOneVndPaymentNotificationForThePayingUser() {
        Payment payment = payment();
        when(repository.existsByTypeAndPaymentId("PAYMENT_SUCCEEDED", 81L)).thenReturn(false);
        when(repository.saveAndFlush(any(UserNotification.class))).thenAnswer(invocation -> {
            UserNotification saved = invocation.getArgument(0);
            saved.setNotificationId(91L);
            return saved;
        });

        TransactionSynchronizationManager.initSynchronization();
        try {
            service.createPaymentSucceeded(payment);
            var synchronization = TransactionSynchronizationManager.getSynchronizations().get(0);
            synchronization.afterCommit();
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }

        var captor = org.mockito.ArgumentCaptor.forClass(UserNotification.class);
        verify(repository).saveAndFlush(captor.capture());
        assertEquals(7L, captor.getValue().getUserId());
        assertEquals(81L, captor.getValue().getPaymentId());
        assertEquals("PAYMENT_SUCCEEDED", captor.getValue().getType());
        assertEquals("Đơn hàng #81 đã thanh toán thành công: 125000 VND.", captor.getValue().getMessage());
    }

    @Test
    void doesNotCreateDuplicateNotificationForSamePayment() {
        when(repository.existsByTypeAndPaymentId("PAYMENT_SUCCEEDED", 81L)).thenReturn(true);

        service.createPaymentSucceeded(payment());

        verify(repository, never()).saveAndFlush(any(UserNotification.class));
    }

    @Test
    void onlyMarksNotificationOwnedByTheCurrentUser() {
        when(repository.findByNotificationIdAndUserId(91L, 7L)).thenReturn(Optional.empty());

        assertThrows(CommonException.class, () -> service.markRead(7L, 91L));

        verify(repository, never()).save(any(UserNotification.class));
    }

    @Test
    void inboxIsScopedToTheRequestedUserAndReturnsUnreadCount() {
        UserNotification notification = new UserNotification();
        notification.setNotificationId(92L);
        notification.setUserId(7L);
        notification.setType("PAYMENT_SUCCEEDED");
        notification.setPaymentId(81L);
        notification.setTitle("Thanh toán thành công");
        notification.setMessage("Đơn hàng #81 đã thanh toán thành công: 125000 VND.");
        when(repository.findTop20ByUserIdOrderByCreatedAtDesc(7L)).thenReturn(List.of(notification));
        when(repository.countByUserIdAndReadAtIsNull(7L)).thenReturn(1L);

        var inbox = service.getInbox(7L);

        assertEquals(1, inbox.items().size());
        assertEquals(1L, inbox.unreadCount());
        assertEquals(81L, inbox.items().get(0).paymentId());
        verify(repository).findTop20ByUserIdOrderByCreatedAtDesc(7L);
    }

    private Payment payment() {
        Payment payment = new Payment();
        payment.setPaymentId(81L);
        payment.setUserId(7L);
        payment.setAmount(new BigDecimal("125000"));
        payment.setCurrency("VND");
        return payment;
    }
}
