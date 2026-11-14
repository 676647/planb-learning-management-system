package fr.skyframe.lms.domain;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

/**
 * A traditional hand-in exercise: students upload a document (e.g. PDF or Word).
 * Graded manually by the teacher.
 */
@Entity
@DiscriminatorValue("ASSIGNMENT")
public class Assignment extends GradedContent {

    /** Up to 100,000 characters; a plain long text column works on PostgreSQL and on H2 (tests). */
    @Column(name = "description", length = 100_000)
    private String description;

    /** Whether the assignment is solved in groups (one submission per group). */
    @Column(name = "assignment_group_work")
    private Boolean groupWork = false;

    @Override
    public ContentType getType() {
        return ContentType.ASSIGNMENT;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public boolean solvedInGroups() {
        return Boolean.TRUE.equals(groupWork);
    }

    public Boolean getGroupWork() {
        return groupWork;
    }

    public void setGroupWork(Boolean groupWork) {
        this.groupWork = groupWork;
    }
}
