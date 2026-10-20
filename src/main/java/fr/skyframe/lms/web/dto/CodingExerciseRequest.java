package fr.skyframe.lms.web.dto;

import fr.skyframe.lms.domain.CodingExercise;
import jakarta.validation.constraints.NotBlank;

import java.time.Instant;

public record CodingExerciseRequest(
        @NotBlank String title,
        Instant visibleFrom,
        Instant visibleUntil,
        Instant deadline,
        Boolean lateSubmissionsAllowed,
        String starterRepositoryUrl,
        Boolean groupWork
) {
    public CodingExercise toEntity() {
        CodingExercise exercise = new CodingExercise();
        exercise.setTitle(title);
        exercise.setVisibleFrom(visibleFrom);
        exercise.setVisibleUntil(visibleUntil);
        exercise.setDeadline(deadline);
        if (lateSubmissionsAllowed != null) exercise.setLateSubmissionsAllowed(lateSubmissionsAllowed);
        exercise.setStarterRepositoryUrl(starterRepositoryUrl);
        if (groupWork != null) exercise.setGroupWork(groupWork);
        return exercise;
    }
}
