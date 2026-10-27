package fr.skyframe.lms.domain;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * An in-app message for one user, created by the notification service from a domain event
 * (e.g. "Grades for 'Hand-in 1' are now available").
 *
 * (eventId, recipient) is unique: RabbitMQ may deliver a message more than once, and this makes
 * sure a repeated delivery does not create the same notification twice.
 */
@Entity
@Table(name = "notifications",
        uniqueConstraints = @UniqueConstraint(columnNames = {"event_id", "recipient_id"}))
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "recipient_id", nullable = false)
    private User recipient;

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(nullable = false, length = 1000)
    private String message;

    /** Where the notification points to, so the frontend can open it. */
    private Long courseId;
    private Long contentId;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(name = "is_read", nullable = false)
    private boolean read = false;

    public Long getId() {
        return id;
    }

    public User getRecipient() {
        return recipient;
    }

    public void setRecipient(User recipient) {
        this.recipient = recipient;
    }

    public UUID getEventId() {
        return eventId;
    }

    public void setEventId(UUID eventId) {
        this.eventId = eventId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Long getCourseId() {
        return courseId;
    }

    public void setCourseId(Long courseId) {
        this.courseId = courseId;
    }

    public Long getContentId() {
        return contentId;
    }

    public void setContentId(Long contentId) {
        this.contentId = contentId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }
}
