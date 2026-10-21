package fr.skyframe.lms.web.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/** teacherId is temporary: once security is added, the grader will be the logged-in user. */
public record GradeRequest(@NotNull Long teacherId, @NotNull @PositiveOrZero Double points, String feedback) {
}
