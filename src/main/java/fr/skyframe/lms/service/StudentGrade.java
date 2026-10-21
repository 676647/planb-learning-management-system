package fr.skyframe.lms.service;

import fr.skyframe.lms.domain.ContentType;

import java.time.Instant;

/**
 * What a student sees about one gradable item. Points and feedback are only filled in
 * once the teacher has published the grades for that item.
 */
public record StudentGrade(Long contentId, String title, ContentType type, Instant deadline,
                           Gradebook.Status status, boolean late,
                           Double points, Integer maxPoints, String feedback) {
}
