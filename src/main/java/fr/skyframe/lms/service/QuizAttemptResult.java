package fr.skyframe.lms.service;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

/**
 * The result of one quiz attempt, shown to the student right after answering.
 * correctAnswer per question is only filled in once correctAnswersShown is true (no attempts left,
 * or a teacher is looking), so students can't copy the solution into their next attempt.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record QuizAttemptResult(Long submissionId, Long quizId, int attemptNumber, int attemptsUsed,
                                Integer maxAttempts, Instant submittedAt, boolean late,
                                double points, Integer maxPoints, boolean correctAnswersShown,
                                List<QuestionResult> questions) {

    /** One question of the attempt. hint is only set for wrong answers when the quiz shows hints. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record QuestionResult(StudentQuestion question, double earnedPoints, boolean correct,
                                 List<String> yourAnswer, String hint, List<String> correctAnswer) {
    }
}
