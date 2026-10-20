package fr.skyframe.lms.domain;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

/**
 * A programming exercise with starter code in a Git repository that students can clone.
 * Visible and hidden test cases for automatic grading will be added in a later step.
 */
@Entity
@DiscriminatorValue("CODING_EXERCISE")
public class CodingExercise extends GradedContent {

    private String starterRepositoryUrl;

    /** Whether the exercise is solved in groups (one submission per group). */
    @Column(name = "exercise_group_work")
    private Boolean groupWork = false;

    @Override
    public ContentType getType() {
        return ContentType.CODING_EXERCISE;
    }

    public String getStarterRepositoryUrl() {
        return starterRepositoryUrl;
    }

    public void setStarterRepositoryUrl(String starterRepositoryUrl) {
        this.starterRepositoryUrl = starterRepositoryUrl;
    }

    public Boolean getGroupWork() {
        return groupWork;
    }

    public void setGroupWork(Boolean groupWork) {
        this.groupWork = groupWork;
    }
}
