package fr.skyframe.lms.web.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Body for creating or updating a course.
 * teacherId is only needed on create, and is temporary: once Spring Security is
 * added, the teacher will be the logged-in user instead.
 */
public record CourseRequest(
        @NotBlank String title,
        String description,
        String semester,
        Long teacherId
) {
}
