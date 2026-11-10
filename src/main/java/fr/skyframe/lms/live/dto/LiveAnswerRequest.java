package fr.skyframe.lms.live.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * @param questionNumber the question being answered (1-based), so a late answer can't land on the next one
 * @param answer         same format as in normal quiz attempts
 */
public record LiveAnswerRequest(@Min(1) int questionNumber, @NotNull List<String> answer) {
}
