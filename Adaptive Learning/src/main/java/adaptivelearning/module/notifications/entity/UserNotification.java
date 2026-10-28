package adaptivelearning.module.notifications.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "user_notifications", indexes = {
        @Index(name = "idx_user_notifications_user_created", columnList = "user_id,created_at")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_notification_payment", columnNames = {"notification_type", "payment_id"})
})
@Getter
@Setter
@NoArgsConstructor
public class UserNotification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long notificationId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "notification_type", nullable = false, length = 40)
    private String type;

    @Column(name = "payment_id", nullable = false)
    private Long paymentId;

    @Column(nullable = false, length = 160)
    private String title;

    @Column(nullable = false, length = 500)
    private String message;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
