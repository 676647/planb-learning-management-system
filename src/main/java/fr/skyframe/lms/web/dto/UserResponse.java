package fr.skyframe.lms.web.dto;

import fr.skyframe.lms.domain.User;

public record UserResponse(Long id, String email, String name) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getName());
    }
}
