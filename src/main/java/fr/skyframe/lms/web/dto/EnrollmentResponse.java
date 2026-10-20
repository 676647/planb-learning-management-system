package fr.skyframe.lms.web.dto;

import fr.skyframe.lms.domain.Enrollment;
import fr.skyframe.lms.domain.Role;

public record EnrollmentResponse(Long id, UserResponse user, CourseResponse course, Role role) {

    public static EnrollmentResponse from(Enrollment e) {
        return new EnrollmentResponse(
                e.getId(),
                UserResponse.from(e.getUser()),
                CourseResponse.from(e.getCourse()),
                e.getRole());
    }
}
