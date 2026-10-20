package fr.skyframe.lms.domain;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

/**
 * An interactive quiz. Attempts are graded automatically.
 * Questions (multiple choice, ordering, matching, fill-in-the-blanks) will be added as a
 * separate entity in a later step.
 */
@Entity
@DiscriminatorValue("QUIZ")
public class Quiz extends GradedContent {

    /** How many attempts a student gets. */
    private Integer maxAttempts = 1;

    /** Whether hints are shown when an answer is wrong. */
    private Boolean showHints = false;

    @Override
    public ContentType getType() {
        return ContentType.QUIZ;
    }

    public Integer getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(Integer maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public Boolean getShowHints() {
        return showHints;
    }

    public void setShowHints(Boolean showHints) {
        this.showHints = showHints;
    }
}
