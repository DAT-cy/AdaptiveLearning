package adaptivelearning.module.notifications.controller;

import adaptivelearning.config.response.DefaultRes;
import adaptivelearning.module.notifications.dto.NotificationInboxResponse;
import adaptivelearning.module.notifications.dto.NotificationResponse;
import adaptivelearning.module.notifications.service.NotificationService;
import adaptivelearning.utils.ClientUtils;
import adaptivelearning.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping(ClientUtils.VERSION + "/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationService notificationService;

    @GetMapping
    public DefaultRes<NotificationInboxResponse> inbox() {
        return DefaultRes.res(200, "Notifications retrieved",
                notificationService.getInbox(SecurityUtils.getCurrentUserId()));
    }

    @PatchMapping("/{id}/read")
    public DefaultRes<NotificationResponse> markRead(@PathVariable Long id) {
        return DefaultRes.res(200, "Notification marked as read",
                notificationService.markRead(SecurityUtils.getCurrentUserId(), id));
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream() {
        return notificationService.subscribe(SecurityUtils.getCurrentUserId());
    }
}
