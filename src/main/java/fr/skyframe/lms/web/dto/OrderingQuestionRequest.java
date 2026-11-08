package fr.skyframe.lms.web.dto;

import fr.skyframe.lms.domain.OrderingQuestion;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.List;

/** items in the CORRECT order; students will see them shuffled. */
public record OrderingQuestionRequest(@NotBlank String prompt, @PositiveOrZero Integer points, String hint,
                                      @NotNull List<String> items) {
    public OrderingQuestion toEntity() {
        OrderingQuestion q = new OrderingQuestion();
        QuestionRequests.common(q, prompt, points, hint);
        q.setItems(items);
        return q;
    }
}
