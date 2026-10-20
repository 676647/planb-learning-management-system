package fr.skyframe.lms.web.dto;

import fr.skyframe.lms.domain.Quiz;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.time.Instant;

public record QuizRequest(
        @NotBlank String title,
        Instant visibleFrom,
        Instant visibleUntil,
        Instant deadline,
        Boolean lateSubmissionsAllowed,
        @Min(1) Integer maxAttempts,
        Boolean showHints
) {
    public Quiz toEntity() {
        Quiz quiz = new Quiz();
        quiz.setTitle(title);
        quiz.setVisibleFrom(visibleFrom);
        quiz.setVisibleUntil(visibleUntil);
        quiz.setDeadline(deadline);
        if (lateSubmissionsAllowed != null) quiz.setLateSubmissionsAllowed(lateSubmissionsAllowed);
        if (maxAttempts != null) quiz.setMaxAttempts(maxAttempts);
        if (showHints != null) quiz.setShowHints(showHints);
        return quiz;
    }
}
