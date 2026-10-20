package fr.skyframe.lms.web.dto;

import fr.skyframe.lms.domain.CourseModule;

import java.util.List;

public record ModuleResponse(Long id, Long courseId, String title, int position, List<Long> requiredModuleIds) {

    public static ModuleResponse from(CourseModule m) {
        List<Long> required = m.getRequires().stream().map(CourseModule::getId).sorted().toList();
        return new ModuleResponse(m.getId(), m.getCourse().getId(), m.getTitle(), m.getPosition(), required);
    }
}
