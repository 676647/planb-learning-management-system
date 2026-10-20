package fr.skyframe.lms.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import fr.skyframe.lms.domain.*;

import java.time.Instant;

/**
 * One response shape for all content types. Fields that don't apply to an item's type
 * are null and left out of the JSON (e.g. a page has no "deadline").
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ContentResponse(
        Long id,
        Long moduleId,
        ContentType type,
        String title,
        int position,
        Instant visibleFrom,
        Instant visibleUntil,
        // Page
        String body,
        // Assignment
        String description,
        // Quiz, CodingExercise, Assignment
        Instant deadline,
        Boolean lateSubmissionsAllowed,
        // Quiz
        Integer maxAttempts,
        Boolean showHints,
        // CodingExercise, Assignment
        Boolean groupWork,
        // CodingExercise
        String starterRepositoryUrl
) {
    public static ContentResponse from(Content c) {
        String body = null, description = null, repo = null;
        Instant deadline = null;
        Boolean late = null, hints = null, group = null;
        Integer attempts = null;

        if (c instanceof Page p) {
            body = p.getBody();
        }
        if (c instanceof GradedContent g) {
            deadline = g.getDeadline();
            late = g.getLateSubmissionsAllowed();
        }
        if (c instanceof Quiz q) {
            attempts = q.getMaxAttempts();
            hints = q.getShowHints();
        }
        if (c instanceof CodingExercise e) {
            repo = e.getStarterRepositoryUrl();
            group = e.getGroupWork();
        }
        if (c instanceof Assignment a) {
            description = a.getDescription();
            group = a.getGroupWork();
        }
        return new ContentResponse(c.getId(), c.getModule().getId(), c.getType(), c.getTitle(), c.getPosition(),
                c.getVisibleFrom(), c.getVisibleUntil(), body, description, deadline, late, attempts, hints,
                group, repo);
    }
}
