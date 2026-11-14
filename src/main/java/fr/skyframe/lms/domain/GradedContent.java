package fr.skyframe.lms.domain;

import jakarta.persistence.Entity;

import java.time.Instant;

/**
 * Common base for content that students hand something in for and that gets graded:
 * quizzes, coding exercises and assignments. These can have a deadline, and the teacher
 * decides whether late submissions are accepted (they are then flagged as late).
 *
 * Wrapper types (Boolean, Integer) are used for subclass fields because, with single-table
 * inheritance, rows of other content types leave these columns empty (NULL).
 */
@Entity
public abstract class GradedContent extends Content {

    private Instant deadline;

    private Boolean lateSubmissionsAllowed = false;

    /** Maximum number of points; null means no upper limit. */
    private Integer maxPoints;

    /** Teachers decide when grades become visible to students. */
    private Boolean gradesPublished = false;

    /** True if a submission at this time would be late. Without a deadline nothing is late. */
    public boolean isLate(Instant submittedAt) {
        return deadline != null && submittedAt.isAfter(deadline);
    }

    /** True if a submission at this time should be accepted. */
    public boolean acceptsSubmissionAt(Instant submittedAt) {
        return !isLate(submittedAt) || Boolean.TRUE.equals(lateSubmissionsAllowed);
    }

    /** True if this item is solved in groups (one submission per group). Overridden by subclasses. */
    public boolean solvedInGroups() {
        return false;
    }

    public Integer getMaxPoints() {
        return maxPoints;
    }

    public void setMaxPoints(Integer maxPoints) {
        this.maxPoints = maxPoints;
    }

    public Boolean getGradesPublished() {
        return gradesPublished;
    }

    public void setGradesPublished(Boolean gradesPublished) {
        this.gradesPublished = gradesPublished;
    }

    public Instant getDeadline() {
        return deadline;
    }

    public void setDeadline(Instant deadline) {
        this.deadline = deadline;
    }

    public Boolean getLateSubmissionsAllowed() {
        return lateSubmissionsAllowed;
    }

    public void setLateSubmissionsAllowed(Boolean lateSubmissionsAllowed) {
        this.lateSubmissionsAllowed = lateSubmissionsAllowed;
    }
}
