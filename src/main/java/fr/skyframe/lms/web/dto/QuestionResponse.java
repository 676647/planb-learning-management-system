package fr.skyframe.lms.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import fr.skyframe.lms.domain.*;

import java.util.List;

/**
 * A question with its solution, for TEACHERS only (students get StudentQuestion instead).
 * Fields that don't apply to the type are left out of the JSON.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record QuestionResponse(Long id, Long quizId, QuestionType type, int position, String prompt, int points,
                               String hint,
                               List<String> options, List<Integer> correctOptions,   // MULTIPLE_CHOICE
                               List<String> items,                                   // ORDERING (correct order)
                               List<MatchingQuestionRequest.Pair> pairs,             // MATCHING
                               String text) {                                        // FILL_IN_BLANKS

    public static QuestionResponse from(Question q) {
        List<String> options = null, items = null;
        List<Integer> correct = null;
        List<MatchingQuestionRequest.Pair> pairs = null;
        String text = null;
        if (q instanceof MultipleChoiceQuestion mc) {
            options = List.copyOf(mc.getOptions());
            correct = mc.getCorrectOptions().stream().sorted().toList();
        } else if (q instanceof OrderingQuestion o) {
            items = List.copyOf(o.getItems());
        } else if (q instanceof MatchingQuestion m) {
            pairs = m.getPairs().stream().map(p -> new MatchingQuestionRequest.Pair(p.getLeft(), p.getRight())).toList();
        } else if (q instanceof FillInBlanksQuestion f) {
            text = f.getText();
        }
        return new QuestionResponse(q.getId(), q.getQuiz().getId(), q.getType(), q.getPosition(), q.getPrompt(),
                q.getPoints(), q.getHint(), options, correct, items, pairs, text);
    }
}
