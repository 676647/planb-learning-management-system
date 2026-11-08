package fr.skyframe.lms.web.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Map;

/**
 * A student's answers: question id → answer in that question type's format, e.g.
 * { "12": ["0","2"], "13": ["Build","Test","Deploy"], "14": ["8080"] }.
 * Unanswered questions may be left out (they score 0).
 */
public record QuizAttemptRequest(@NotNull Map<Long, List<String>> answers) {
}
