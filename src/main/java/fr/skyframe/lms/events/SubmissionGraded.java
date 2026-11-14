package fr.skyframe.lms.events;

import java.time.Instant;
import java.util.UUID;

/** A teacher graded (or regraded) a submission. Students only see it once grades are published. */
public record SubmissionGraded(UUID eventId, Instant occurredAt,
                               Long submissionId, Long courseId, Long contentId,
                               double points, Long teacherId) implements DomainEvent {

    public static final String ROUTING_KEY = "submission.graded";

    @Override
    public String routingKey() {
        return ROUTING_KEY;
    }
}
