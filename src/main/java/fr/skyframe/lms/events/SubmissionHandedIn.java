package fr.skyframe.lms.events;

import java.time.Instant;
import java.util.UUID;

/** A student handed in work for a quiz, coding exercise or assignment. late = after the deadline. */
public record SubmissionHandedIn(UUID eventId, Instant occurredAt,
                                 Long submissionId, Long courseId, Long contentId, String contentTitle,
                                 Long studentId, Long groupId, boolean late) implements DomainEvent {

    public static final String ROUTING_KEY = "submission.handed-in";

    @Override
    public String routingKey() {
        return ROUTING_KEY;
    }
}
