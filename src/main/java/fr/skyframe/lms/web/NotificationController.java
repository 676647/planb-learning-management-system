package fr.skyframe.lms.web;

import fr.skyframe.lms.security.CurrentUser;
import fr.skyframe.lms.service.NotificationService;
import fr.skyframe.lms.web.dto.NotificationResponse;
import fr.skyframe.lms.web.dto.NotificationsResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/** The logged-in user's notifications. Users only ever see and change their own. */
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final CurrentUser currentUser;

    public NotificationController(NotificationService notificationService, CurrentUser currentUser) {
        this.notificationService = notificationService;
        this.currentUser = currentUser;
    }

    @GetMapping
    public NotificationsResponse mine() {
        Long me = currentUser.id();
        return new NotificationsResponse(notificationService.unreadCount(me),
                notificationService.listFor(me).stream().map(NotificationResponse::from).toList());
    }

    @PutMapping("/{id}/read")
    public NotificationResponse markRead(@PathVariable Long id) {
        return NotificationResponse.from(notificationService.markRead(id, currentUser.id()));
    }

    @PutMapping("/read-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markAllRead() {
        notificationService.markAllRead(currentUser.id());
    }
}
