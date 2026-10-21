package fr.skyframe.lms.web.dto;

import fr.skyframe.lms.domain.StudentGroup;

import java.util.Comparator;
import java.util.List;

public record GroupResponse(Long id, Long courseId, String name, List<UserResponse> members) {

    public static GroupResponse from(StudentGroup g) {
        List<UserResponse> members = g.getMembers().stream()
                .map(UserResponse::from)
                .sorted(Comparator.comparing(UserResponse::id))
                .toList();
        return new GroupResponse(g.getId(), g.getCourse().getId(), g.getName(), members);
    }
}
