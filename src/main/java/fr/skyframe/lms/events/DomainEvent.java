package fr.skyframe.lms.events;

import java.time.Instant;
import java.util.UUID;

/**
 * Something that happened in the LMS that other components may want to react to.
 *
 * Events are published by the services, sent to RabbitMQ (exchange "lms.events") after the
 * database transaction has committed, and consumed by listeners such as NotificationListener.
 * Any other component can bind its own queue to the exchange and receive them as JSON.
 *
 * Each event type is an immutable record. "sealed" lists every type that exists, so a
 * switch over DomainEvent must handle them all.
 */
public sealed interface DomainEvent
        permits SubmissionHandedIn, SubmissionGraded, GradesPublished, UserEnrolled {

    /** Unique per event, so consumers can recognise a message they have already handled. */
    UUID eventId();

    Instant occurredAt();

    /** The RabbitMQ routing key, e.g. "submission.handed-in". Consumers can bind to patterns like "submission.*". */
    String routingKey();
}
