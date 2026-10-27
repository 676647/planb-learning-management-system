package fr.skyframe.lms.web.dto;

import fr.skyframe.lms.domain.Notification;

import java.time.Instant;

public record NotificationResponse(Long id, String message, Long courseId, Long contentId,
                                   Instant createdAt, boolean read) {

    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(n.getId(), n.getMessage(), n.getCourseId(), n.getContentId(),
                n.getCreatedAt(), n.isRead());
    }
}
