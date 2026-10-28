package adaptivelearning.module.notifications.dto;

import java.time.LocalDateTime;

public record NotificationResponse(
        Long notificationId,
        String type,
        String title,
        String message,
        Long paymentId,
        boolean read,
        LocalDateTime createdAt) {}
