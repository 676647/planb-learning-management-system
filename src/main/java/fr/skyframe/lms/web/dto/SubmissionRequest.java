package fr.skyframe.lms.web.dto;

import jakarta.validation.constraints.NotNull;

/**
 * studentId is temporary: once security is added, the submitter will be the logged-in user.
 * answer is free text or a link (e.g. to a Git repository).
 */
public record SubmissionRequest(@NotNull Long studentId, String answer) {
}
