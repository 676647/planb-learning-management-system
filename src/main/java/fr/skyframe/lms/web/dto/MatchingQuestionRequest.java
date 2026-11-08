package fr.skyframe.lms.web.dto;

import fr.skyframe.lms.domain.MatchPair;
import fr.skyframe.lms.domain.MatchingQuestion;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.List;

/** The correct pairs; students will see the right-hand side shuffled. */
public record MatchingQuestionRequest(@NotBlank String prompt, @PositiveOrZero Integer points, String hint,
                                      @NotNull List<Pair> pairs) {

    public record Pair(String left, String right) {
    }

    public MatchingQuestion toEntity() {
        MatchingQuestion q = new MatchingQuestion();
        QuestionRequests.common(q, prompt, points, hint);
        q.setPairs(pairs.stream().map(p -> new MatchPair(p.left(), p.right())).toList());
        return q;
    }
}
