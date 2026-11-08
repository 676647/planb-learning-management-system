package fr.skyframe.lms.web.dto;

import fr.skyframe.lms.domain.Question;

/** The fields every question request has. */
final class QuestionRequests {

    private QuestionRequests() {
    }

    static void common(Question q, String prompt, Integer points, String hint) {
        q.setPrompt(prompt);
        q.setPoints(points == null ? 1 : points);
        q.setHint(hint == null || hint.isBlank() ? null : hint.trim());
    }
}
