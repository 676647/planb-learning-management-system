package fr.skyframe.lms.web.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/** The grader is the logged-in user. */
public record GradeRequest(@NotNull @PositiveOrZero Double points, String feedback) {
}
