package fr.skyframe.lms.service;

import fr.skyframe.lms.domain.*;
import fr.skyframe.lms.events.*;
import fr.skyframe.lms.repository.EnrollmentRepository;
import fr.skyframe.lms.repository.NotificationRepository;
import fr.skyframe.lms.repository.StudentGroupRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Turns domain events into in-app notifications. This is the consumer side of the messaging:
 * it is called by NotificationListener (RabbitMQ) or LocalEventDelivery (tests).
 *
 * Who is told what:
 * - hand-in:         the student (and their group) get a receipt; teachers are told if it was late
 * - grades published: students whose work was graded are told grades are available
 * - enrolled:         the added user is told which course they were added to
 * - graded:           nobody yet (grades stay hidden until published), only logged
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notifications;
    private final EnrollmentRepository enrollments;
    private final StudentGroupRepository groups;
    private final UserService userService;
    private final GradingService gradingService;
    private final Clock clock;

    public NotificationService(NotificationRepository notifications, EnrollmentRepository enrollments,
                               StudentGroupRepository groups, UserService userService,
                               GradingService gradingService, Clock clock) {
        this.notifications = notifications;
        this.enrollments = enrollments;
        this.groups = groups;
        this.userService = userService;
        this.gradingService = gradingService;
        this.clock = clock;
    }

    /**
     * Handles one event. REQUIRES_NEW: in local mode this runs right after another transaction
     * has committed, so it needs a fresh transaction of its own to save anything.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handle(DomainEvent event) {
        log.info("Event {}: {}", event.routingKey(), event);
        switch (event) {
            case SubmissionHandedIn e -> onHandedIn(e);
            case GradesPublished e -> onGradesPublished(e);
            case UserEnrolled e -> onEnrolled(e);
            case SubmissionGraded e -> { /* nothing to tell students before grades are published */ }
        }
    }

    private void onHandedIn(SubmissionHandedIn e) {
        User student = userService.get(e.studentId());
        String lateNote = e.late() ? " (after the deadline, flagged as late)" : "";

        // Receipt for the student, or for every member if it was handed in for a group
        Set<User> recipients = new LinkedHashSet<>();
        recipients.add(student);
        if (e.groupId() != null) {
            groups.findById(e.groupId()).ifPresent(g -> recipients.addAll(g.getMembers()));
        }
        for (User r : recipients) {
            String who = r.getId().equals(student.getId()) ? "You" : student.getName() + " (your group)";
            notify(r, e, who + " handed in '" + e.contentTitle() + "'" + lateNote + ".", e.courseId(), e.contentId());
        }

        // Teachers only hear about late hand-ins, so they aren't flooded with messages
        if (e.late()) {
            for (User teacher : teachersOf(e.courseId())) {
                notify(teacher, e, student.getName() + " handed in '" + e.contentTitle()
                        + "' after the deadline.", e.courseId(), e.contentId());
            }
        }
    }

    private void onGradesPublished(GradesPublished e) {
        if (!e.published()) {
            return;
        }
        // Everyone whose (group's) latest submission for this item has a grade
        for (Gradebook.StudentRow row : gradingService.gradebook(e.courseId()).students()) {
            boolean graded = row.entries().stream().anyMatch(entry ->
                    entry.contentId().equals(e.contentId()) && entry.status() == Gradebook.Status.GRADED);
            if (graded) {
                notify(userService.get(row.userId()), e, "Your grade for '" + e.contentTitle() + "' is now available.",
                        e.courseId(), e.contentId());
            }
        }
    }

    private void onEnrolled(UserEnrolled e) {
        String role = e.role() == Role.TEACHER ? "a teacher" : "a student";
        notify(userService.get(e.userId()), e, "You were added to '" + e.courseTitle() + "' as " + role + ".",
                e.courseId(), null);
    }

    private Collection<User> teachersOf(Long courseId) {
        return enrollments.findByCourseId(courseId).stream()
                .filter(en -> en.getRole() == Role.TEACHER)
                .map(Enrollment::getUser)
                .toList();
    }

    private void notify(User recipient, DomainEvent event, String message, Long courseId, Long contentId) {
        if (notifications.existsByEventIdAndRecipientId(event.eventId(), recipient.getId())) {
            return; // the same message was delivered twice; we already handled it
        }
        Notification n = new Notification();
        n.setRecipient(recipient);
        n.setEventId(event.eventId());
        n.setMessage(message);
        n.setCourseId(courseId);
        n.setContentId(contentId);
        n.setCreatedAt(clock.instant());
        notifications.save(n);
    }

    // --- reading notifications (used by NotificationController) ---

    @Transactional(readOnly = true)
    public List<Notification> listFor(Long userId) {
        return notifications.findTop50ByRecipientIdOrderByCreatedAtDescIdDesc(userId);
    }

    @Transactional(readOnly = true)
    public long unreadCount(Long userId) {
        return notifications.countByRecipientIdAndReadFalse(userId);
    }

    @Transactional
    public Notification markRead(Long notificationId, Long userId) {
        Notification n = notifications.findByIdAndRecipientId(notificationId, userId)
                .orElseThrow(() -> new NotFoundException("Notification " + notificationId + " not found"));
        n.setRead(true);
        return n;
    }

    @Transactional
    public void markAllRead(Long userId) {
        notifications.findByRecipientIdAndReadFalse(userId).forEach(n -> n.setRead(true));
    }
}
