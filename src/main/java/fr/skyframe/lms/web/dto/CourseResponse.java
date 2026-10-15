package fr.skyframe.lms.web.dto;

import fr.skyframe.lms.domain.Course;

public record CourseResponse(Long id, String title, String description, String semester) {

    public static CourseResponse from(Course course) {
        return new CourseResponse(course.getId(), course.getTitle(), course.getDescription(), course.getSemester());
    }
}
