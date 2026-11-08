package fr.skyframe.lms.domain;

import jakarta.persistence.*;

import java.util.*;

/**
 * Pick the right option(s). With more than one correct option, students may tick several.
 * Answer format: the indexes of the chosen options, e.g. ["0", "2"].
 * Graded all-or-nothing: exactly the correct set of options gives full points.
 */
@Entity
@DiscriminatorValue("MULTIPLE_CHOICE")
public class MultipleChoiceQuestion extends Question {

    @ElementCollection
    @CollectionTable(name = "question_options", joinColumns = @JoinColumn(name = "question_id"))
    @OrderColumn(name = "position")
    @Column(name = "option_text", length = 1000)
    private List<String> options = new ArrayList<>();

    /** Indexes into {@link #options}. */
    @ElementCollection
    @CollectionTable(name = "question_correct_options", joinColumns = @JoinColumn(name = "question_id"))
    @Column(name = "option_index")
    private Set<Integer> correctOptions = new HashSet<>();

    @Override
    public QuestionType getType() {
        return QuestionType.MULTIPLE_CHOICE;
    }

    /** Whether students may tick more than one option. */
    public boolean multipleAnswers() {
        return correctOptions.size() > 1;
    }

    @Override
    public void validate() {
        if (options.size() < 2 || options.stream().anyMatch(o -> clean(o).isEmpty())) {
            throw new IllegalArgumentException("A multiple-choice question needs at least two non-empty options");
        }
        if (correctOptions.isEmpty() || correctOptions.stream().anyMatch(i -> i == null || i < 0 || i >= options.size())) {
            throw new IllegalArgumentException("Mark at least one of the options as correct");
        }
    }

    @Override
    public double score(List<String> answer) {
        Set<Integer> chosen = new HashSet<>();
        for (String a : answer == null ? List.<String>of() : answer) {
            try {
                chosen.add(Integer.parseInt(clean(a)));
            } catch (NumberFormatException e) {
                // not an option index: ignore
            }
        }
        return chosen.equals(correctOptions) ? 1.0 : 0.0;
    }

    @Override
    public List<String> correctAnswer() {
        return correctOptions.stream().sorted().map(String::valueOf).toList();
    }

    public List<String> getOptions() {
        return options;
    }

    public void setOptions(List<String> options) {
        this.options = new ArrayList<>(options);
    }

    public Set<Integer> getCorrectOptions() {
        return correctOptions;
    }

    public void setCorrectOptions(Set<Integer> correctOptions) {
        this.correctOptions = new HashSet<>(correctOptions);
    }
}
