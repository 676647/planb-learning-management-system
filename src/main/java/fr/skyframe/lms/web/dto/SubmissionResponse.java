package fr.skyframe.lms.web.dto;

import fr.skyframe.lms.domain.Submission;

import java.time.Instant;

/** file is null when nothing was uploaded; download it from /api/submissions/{id}/file. */
public record SubmissionResponse(Long id, Long contentId, UserResponse submittedBy, Long groupId,
                                 Instant submittedAt, boolean late, String answer, FileResponse file) {

    public static SubmissionResponse from(Submission s) {
        return new SubmissionResponse(s.getId(), s.getContent().getId(), UserResponse.from(s.getSubmittedBy()),
                s.getGroup() != null ? s.getGroup().getId() : null,
                s.getSubmittedAt(), s.isLate(), s.getAnswer(), FileResponse.from(s.getFile()));
    }
}
