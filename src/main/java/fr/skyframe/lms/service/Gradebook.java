package fr.skyframe.lms.service;

import fr.skyframe.lms.domain.ContentType;

import java.time.Instant;
import java.util.List;

/**
 * The teacher's overview of a course (US3): every student against every gradable item,
 * plus per-item statistics, so teachers can spot topics or students that need follow-up.
 */
public record Gradebook(Long courseId, List<Item> items, List<StudentRow> students) {

    public enum Status { NOT_SUBMITTED, SUBMITTED, GRADED }

    /** One gradable item, with statistics over all students. */
    public record Item(Long contentId, String title, ContentType type, Instant deadline, Integer maxPoints,
                       int submittedCount, int gradedCount, Double averagePoints) {
    }

    /** One student's results across all items, in the same order as {@code items}. */
    public record StudentRow(Long userId, String name, String email, List<Entry> entries, double totalPoints) {
    }

    /** One student's result for one item. points is null unless graded. */
    public record Entry(Long contentId, Status status, boolean late, Double points, Long submissionId) {
    }
}
