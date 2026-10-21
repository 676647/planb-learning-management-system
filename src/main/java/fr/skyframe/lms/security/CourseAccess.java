package fr.skyframe.lms.security;

import fr.skyframe.lms.domain.Role;
import fr.skyframe.lms.domain.Submission;
import fr.skyframe.lms.repository.*;
import fr.skyframe.lms.service.NotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * The per-course permission rules, used in @PreAuthorize annotations on the controllers, e.g.
 * {@code @PreAuthorize("@access.isTeacherOfCourse(#id)")}.
 *
 * Roles are assigned per course (the same user can teach one course and take another), so
 * every rule first finds the course that a module/content/group/submission belongs to and
 * then checks the logged-in user's enrollment in that course.
 *
 * An unknown id throws NotFoundException, so the client gets 404 rather than 403.
 */
@Component("access")
@Transactional(readOnly = true)
public class CourseAccess {

    private final CurrentUser currentUser;
    private final EnrollmentRepository enrollments;
    private final CourseRepository courses;
    private final CourseModuleRepository modules;
    private final ContentRepository contents;
    private final StudentGroupRepository groups;
    private final SubmissionRepository submissions;

    public CourseAccess(CurrentUser currentUser, EnrollmentRepository enrollments, CourseRepository courses,
                        CourseModuleRepository modules, ContentRepository contents,
                        StudentGroupRepository groups, SubmissionRepository submissions) {
        this.currentUser = currentUser;
        this.enrollments = enrollments;
        this.courses = courses;
        this.modules = modules;
        this.contents = contents;
        this.groups = groups;
        this.submissions = submissions;
    }

    // --- courses ---

    public boolean isTeacherOfCourse(Long courseId) {
        requireCourse(courseId);
        return enrollments.existsByUserIdAndCourseIdAndRole(currentUser.id(), courseId, Role.TEACHER);
    }

    /** Teacher or student of the course. */
    public boolean isMemberOfCourse(Long courseId) {
        requireCourse(courseId);
        return enrollments.existsByUserIdAndCourseId(currentUser.id(), courseId);
    }

    // --- modules and contents ---

    public boolean isTeacherOfModule(Long moduleId) {
        return isTeacherOfCourse(courseOfModule(moduleId));
    }

    public boolean isMemberOfModule(Long moduleId) {
        return isMemberOfCourse(courseOfModule(moduleId));
    }

    public boolean isTeacherOfContent(Long contentId) {
        return isTeacherOfCourse(courseOfContent(contentId));
    }

    public boolean isMemberOfContent(Long contentId) {
        return isMemberOfCourse(courseOfContent(contentId));
    }

    // --- groups ---

    public boolean isTeacherOfGroup(Long groupId) {
        return isTeacherOfCourse(courseOfGroup(groupId));
    }

    public boolean isMemberOfGroupCourse(Long groupId) {
        return isMemberOfCourse(courseOfGroup(groupId));
    }

    // --- submissions and grades ---

    public boolean isTeacherOfSubmission(Long submissionId) {
        return isTeacherOfCourse(submission(submissionId).getContent().getModule().getCourse().getId());
    }

    /** Teachers of the course, and the student (or group members) the submission belongs to. */
    public boolean canSeeSubmission(Long submissionId) {
        Submission s = submission(submissionId);
        return isTeacherOfSubmission(submissionId) || s.countsFor(currentUser.id());
    }

    /** Teachers always; students only for their own submission, and only once grades are published. */
    public boolean canSeeGrade(Long submissionId) {
        Submission s = submission(submissionId);
        if (isTeacherOfSubmission(submissionId)) {
            return true;
        }
        return s.countsFor(currentUser.id()) && Boolean.TRUE.equals(s.getContent().getGradesPublished());
    }

    // --- the user themselves ---

    public boolean isMe(Long userId) {
        return currentUser.id().equals(userId);
    }

    public boolean isMyEmail(String email) {
        return email != null && currentUser.get().getEmail().equalsIgnoreCase(email.trim());
    }

    // --- lookups ---

    private void requireCourse(Long courseId) {
        if (!courses.existsById(courseId)) {
            throw new NotFoundException("Course " + courseId + " not found");
        }
    }

    private Long courseOfModule(Long moduleId) {
        return modules.findById(moduleId)
                .orElseThrow(() -> new NotFoundException("Module " + moduleId + " not found"))
                .getCourse().getId();
    }

    private Long courseOfContent(Long contentId) {
        return contents.findById(contentId)
                .orElseThrow(() -> new NotFoundException("Content " + contentId + " not found"))
                .getModule().getCourse().getId();
    }

    private Long courseOfGroup(Long groupId) {
        return groups.findById(groupId)
                .orElseThrow(() -> new NotFoundException("Group " + groupId + " not found"))
                .getCourse().getId();
    }

    private Submission submission(Long submissionId) {
        return submissions.findById(submissionId)
                .orElseThrow(() -> new NotFoundException("Submission " + submissionId + " not found"));
    }
}
