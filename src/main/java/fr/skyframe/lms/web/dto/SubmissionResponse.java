package fr.skyframe.lms.web.dto;

import fr.skyframe.lms.domain.Submission;

import java.time.Instant;

public record SubmissionResponse(Long id, Long contentId, UserResponse submittedBy, Long groupId,
                                 Instant submittedAt, boolean late, String answer) {

    public static SubmissionResponse from(Submission s) {
        return new SubmissionResponse(s.getId(), s.getContent().getId(), UserResponse.from(s.getSubmittedBy()),
                s.getGroup() != null ? s.getGroup().getId() : null,
                s.getSubmittedAt(), s.isLate(), s.getAnswer());
    }
}
