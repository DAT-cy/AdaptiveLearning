package adaptivelearning.module.notifications.dto;

import java.util.List;

public record NotificationInboxResponse(List<NotificationResponse> items, long unreadCount) {}
