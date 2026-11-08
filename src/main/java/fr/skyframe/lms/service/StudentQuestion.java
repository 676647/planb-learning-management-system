package fr.skyframe.lms.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import fr.skyframe.lms.domain.QuestionType;

import java.util.List;

/**
 * A question as students see it: everything needed to answer it, but never the solution.
 * Fields that don't apply to the type are null and left out of the JSON.
 *
 * - MULTIPLE_CHOICE: options (answer with their indexes); multipleAnswers says if several may be ticked
 * - ORDERING:        items, shuffled (answer with the texts in your order)
 * - MATCHING:        leftItems in order, rightOptions shuffled (answer with one right text per left item)
 * - FILL_IN_BLANKS:  textParts around the blanks; blankCount = textParts.size() - 1
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record StudentQuestion(Long id, int position, QuestionType type, String prompt, int points,
                              List<String> options, Boolean multipleAnswers,
                              List<String> items,
                              List<String> leftItems, List<String> rightOptions,
                              List<String> textParts, Integer blankCount) {
}
