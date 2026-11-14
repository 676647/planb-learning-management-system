package fr.skyframe.lms.live.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * Shown after a question closes.
 *
 * @param correctAnswer same format as answers: option numbers (from 0) for multiple choice,
 *                      items in order, the right side per left side, or the blanks
 * @param optionCounts  multiple choice only: how many players picked each option
 * @param fullyCorrect  players with a completely correct answer
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record LiveReveal(List<String> correctAnswer, List<Integer> optionCounts, int fullyCorrect, int answered) {
}
