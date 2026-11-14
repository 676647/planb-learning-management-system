package fr.skyframe.lms.web.dto;

import fr.skyframe.lms.domain.MultipleChoiceQuestion;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.List;
import java.util.Set;

/** correctOptions are indexes into options, e.g. options ["A","B","C"], correctOptions [0, 2]. */
public record MultipleChoiceQuestionRequest(@NotBlank String prompt, @PositiveOrZero Integer points, String hint,
                                            @NotNull List<String> options, @NotNull Set<Integer> correctOptions) {
    public MultipleChoiceQuestion toEntity() {
        MultipleChoiceQuestion q = new MultipleChoiceQuestion();
        QuestionRequests.common(q, prompt, points, hint);
        q.setOptions(options);
        q.setCorrectOptions(correctOptions);
        return q;
    }
}
