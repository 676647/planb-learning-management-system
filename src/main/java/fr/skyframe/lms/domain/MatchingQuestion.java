package fr.skyframe.lms.domain;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Match each item on the left with one on the right. Students see the right-hand side shuffled.
 * Answer format: for each left item (in order), the right text the student chose.
 * Partial credit: the share of correctly matched pairs.
 */
@Entity
@DiscriminatorValue("MATCHING")
public class MatchingQuestion extends Question {

    @ElementCollection
    @CollectionTable(name = "question_match_pairs", joinColumns = @JoinColumn(name = "question_id"))
    @OrderColumn(name = "position")
    private List<MatchPair> pairs = new ArrayList<>();

    @Override
    public QuestionType getType() {
        return QuestionType.MATCHING;
    }

    @Override
    public void validate() {
        if (pairs.size() < 2 || pairs.stream().anyMatch(p -> clean(p.getLeft()).isEmpty() || clean(p.getRight()).isEmpty())) {
            throw new IllegalArgumentException("A matching question needs at least two complete pairs");
        }
        if (new HashSet<>(pairs.stream().map(p -> clean(p.getLeft())).toList()).size() != pairs.size()) {
            throw new IllegalArgumentException("The left-hand items of a matching question must all be different");
        }
    }

    @Override
    public double score(List<String> answer) {
        if (answer == null) {
            return 0;
        }
        int right = 0;
        for (int i = 0; i < pairs.size() && i < answer.size(); i++) {
            if (clean(pairs.get(i).getRight()).equals(clean(answer.get(i)))) {
                right++;
            }
        }
        return (double) right / pairs.size();
    }

    @Override
    public List<String> correctAnswer() {
        return pairs.stream().map(MatchPair::getRight).toList();
    }

    public List<MatchPair> getPairs() {
        return pairs;
    }

    public void setPairs(List<MatchPair> pairs) {
        this.pairs = new ArrayList<>(pairs);
    }
}
