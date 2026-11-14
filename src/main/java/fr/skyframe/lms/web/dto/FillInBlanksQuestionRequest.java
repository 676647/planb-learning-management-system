package fr.skyframe.lms.web.dto;

import fr.skyframe.lms.domain.FillInBlanksQuestion;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

/** text marks each blank's answer(s) with [[ ]], alternatives separated by |, e.g. "Port [[8080]]". */
public record FillInBlanksQuestionRequest(@NotBlank String prompt, @PositiveOrZero Integer points, String hint,
                                          @NotBlank String text) {
    public FillInBlanksQuestion toEntity() {
        FillInBlanksQuestion q = new FillInBlanksQuestion();
        QuestionRequests.common(q, prompt, points, hint);
        q.setText(text);
        return q;
    }
}
