package fr.skyframe.lms.web.dto;

/**
 * A submission together with its grade (null if not graded yet).
 * Used for the teacher's grading queue, so the frontend needs only one request.
 */
public record SubmissionWithGradeResponse(SubmissionResponse submission, GradeResponse grade) {
}
