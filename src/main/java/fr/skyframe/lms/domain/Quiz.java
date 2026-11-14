package fr.skyframe.lms.domain;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

/**
 * An interactive quiz made of questions (multiple choice, ordering, matching, fill in the blanks).
 * Attempts are graded automatically by QuizService; the best attempt counts.
 * Results are shown to students right away, so gradesPublished starts as true for quizzes.
 */
@Entity
@DiscriminatorValue("QUIZ")
public class Quiz extends GradedContent {

    /** How many attempts a student gets. */
    private Integer maxAttempts = 1;

    /** Whether hints are shown when an answer is wrong. */
    private Boolean showHints = false;

    public Quiz() {
        setGradesPublished(true);
    }

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
