package fr.skyframe.lms.web.dto;

import java.util.List;

/** The logged-in user's newest notifications plus how many are unread. */
public record NotificationsResponse(long unread, List<NotificationResponse> items) {
}
