package fr.skyframe.lms.web.dto;

import fr.skyframe.lms.domain.Grade;

import java.time.Instant;

public record GradeResponse(Long id, Long submissionId, double points, String feedback,
                            Long gradedById, Instant gradedAt) {

    public static GradeResponse from(Grade g) {
        return new GradeResponse(g.getId(), g.getSubmission().getId(), g.getPoints(), g.getFeedback(),
                g.getGradedBy() != null ? g.getGradedBy().getId() : null, g.getGradedAt());
    }
}
