package adaptivelearning.module.notifications.repository;

import adaptivelearning.module.notifications.entity.UserNotification;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserNotificationRepository extends JpaRepository<UserNotification, Long> {
    List<UserNotification> findTop20ByUserIdOrderByCreatedAtDesc(Long userId);
    long countByUserIdAndReadAtIsNull(Long userId);
    Optional<UserNotification> findByNotificationIdAndUserId(Long notificationId, Long userId);
    boolean existsByTypeAndPaymentId(String type, Long paymentId);
}
