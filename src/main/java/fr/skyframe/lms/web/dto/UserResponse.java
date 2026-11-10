package fr.skyframe.lms.web.dto;

import fr.skyframe.lms.domain.AccountRole;
import fr.skyframe.lms.domain.User;

/** accountRole: STUDENT, TEACHER (may create courses) or ADMIN. */
public record UserResponse(Long id, String email, String name, AccountRole accountRole) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getName(), user.getAccountRole());
    }
}
