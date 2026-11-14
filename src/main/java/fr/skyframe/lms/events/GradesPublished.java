package fr.skyframe.lms.events;

import java.time.Instant;
import java.util.UUID;

/** A teacher made the grades of an item visible to students (published = true) or hid them again. */
public record GradesPublished(UUID eventId, Instant occurredAt,
                              Long courseId, Long contentId, String contentTitle,
                              boolean published) implements DomainEvent {

    public static final String ROUTING_KEY = "grades.published";

    @Override
    public String routingKey() {
        return ROUTING_KEY;
    }
}
