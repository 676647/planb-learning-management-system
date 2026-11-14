package fr.skyframe.lms.web.dto;

import jakarta.validation.constraints.NotBlank;

/** Body for creating or updating a course. The teacher is the logged-in user. */
public record CourseRequest(
        @NotBlank String title,
        String description,
        String semester
) {
}
