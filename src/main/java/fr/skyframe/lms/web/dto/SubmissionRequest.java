package fr.skyframe.lms.web.dto;

/** answer is free text or a link (e.g. to a Git repository). The submitter is the logged-in user. */
public record SubmissionRequest(String answer) {
}
