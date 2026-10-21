package fr.skyframe.lms.service;

import fr.skyframe.lms.domain.*;
import fr.skyframe.lms.repository.EnrollmentRepository;
import fr.skyframe.lms.repository.SubmissionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

/**
 * Students hand in work for quizzes, coding exercises and assignments.
 *
 * Rules:
 * - only students of the course can submit, and only while the item is visible;
 * - after the deadline, a submission is rejected unless the teacher allows late submissions,
 *   in which case it is accepted but flagged as late;
 * - for group work the student must be in a group, and the submission counts for the whole group;
 * - quizzes limit the number of attempts.
 */
@Service
@Transactional
public class SubmissionService {

    private final SubmissionRepository submissions;
    private final ContentService contentService;
    private final UserService userService;
    private final EnrollmentRepository enrollments;
    private final GroupService groupService;
    private final Clock clock;

    public SubmissionService(SubmissionRepository submissions, ContentService contentService,
                             UserService userService, EnrollmentRepository enrollments,
                             GroupService groupService, Clock clock) {
        this.submissions = submissions;
        this.contentService = contentService;
        this.userService = userService;
        this.enrollments = enrollments;
        this.groupService = groupService;
        this.clock = clock;
    }

    public Submission submit(Long contentId, Long studentId, String answer) {
        GradedContent item = contentService.getGraded(contentId);
        Long courseId = item.getModule().getCourse().getId();
        User student = userService.get(studentId);

        if (!enrollments.existsByUserIdAndCourseIdAndRole(studentId, courseId, Role.STUDENT)) {
            throw new ForbiddenException("Only students of course " + courseId + " can submit");
        }
        Instant now = clock.instant();
        if (!item.isVisibleAt(now)) {
            throw new ForbiddenException("'" + item.getTitle() + "' is not available right now");
        }
        if (!item.acceptsSubmissionAt(now)) {
            throw new ConflictException("The deadline for '" + item.getTitle() + "' has passed");
        }
        if (item instanceof Quiz quiz) {
            long attempts = submissions.countByContentIdAndSubmittedById(contentId, studentId);
            if (quiz.getMaxAttempts() != null && attempts >= quiz.getMaxAttempts()) {
                throw new ConflictException("No attempts left for '" + quiz.getTitle() + "'");
            }
        }

        Submission submission = new Submission();
        submission.setContent(item);
        submission.setSubmittedBy(student);
        submission.setSubmittedAt(now);
        submission.setLate(item.isLate(now));
        submission.setAnswer(answer);
        if (item.solvedInGroups()) {
            StudentGroup group = groupService.groupOf(courseId, studentId)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "'" + item.getTitle() + "' is group work; join a group first"));
            submission.setGroup(group);
        }
        return submissions.save(submission);
    }

    @Transactional(readOnly = true)
    public Submission get(Long id) {
        return submissions.findById(id)
                .orElseThrow(() -> new NotFoundException("Submission " + id + " not found"));
    }

    /** All submissions for an item, newest first (the teacher's grading queue). */
    @Transactional(readOnly = true)
    public List<Submission> listForContent(Long contentId) {
        contentService.getGraded(contentId);
        return submissions.findByContentIdOrderBySubmittedAtDesc(contentId);
    }
}
