package fr.skyframe.lms.domain;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Put items in the right order (e.g. the phases of a build pipeline). Students see them shuffled.
 * Answer format: the item texts in the order the student chose.
 * Partial credit: the share of items in the right position.
 */
@Entity
@DiscriminatorValue("ORDERING")
public class OrderingQuestion extends Question {

    /** The items in the correct order. */
    @ElementCollection
    @CollectionTable(name = "question_order_items", joinColumns = @JoinColumn(name = "question_id"))
    @OrderColumn(name = "position")
    @Column(name = "item_text", length = 1000)
    private List<String> items = new ArrayList<>();

    @Override
    public QuestionType getType() {
        return QuestionType.ORDERING;
    }

    @Override
    public void validate() {
        if (items.size() < 2 || items.stream().anyMatch(i -> clean(i).isEmpty())) {
            throw new IllegalArgumentException("An ordering question needs at least two non-empty items");
        }
        // Students answer with the texts, so each text must identify exactly one item
        if (new HashSet<>(items.stream().map(Question::clean).toList()).size() != items.size()) {
            throw new IllegalArgumentException("The items of an ordering question must all be different");
        }
    }

    @Override
    public double score(List<String> answer) {
        if (answer == null) {
            return 0;
        }
        int right = 0;
        for (int i = 0; i < items.size() && i < answer.size(); i++) {
            if (clean(items.get(i)).equals(clean(answer.get(i)))) {
                right++;
            }
        }
        return (double) right / items.size();
    }

    @Override
    public List<String> correctAnswer() {
        return List.copyOf(items);
    }

    public List<String> getItems() {
        return items;
    }

    public void setItems(List<String> items) {
        this.items = new ArrayList<>(items);
    }
}
