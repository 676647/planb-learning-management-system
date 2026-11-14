package fr.skyframe.lms.events;

import fr.skyframe.lms.domain.Role;

import java.time.Instant;
import java.util.UUID;

/** A teacher added a user to a course with a role. */
public record UserEnrolled(UUID eventId, Instant occurredAt,
                           Long courseId, String courseTitle, Long userId, Role role) implements DomainEvent {

    public static final String ROUTING_KEY = "enrollment.created";

    @Override
    public String routingKey() {
        return ROUTING_KEY;
    }
}
