package fr.skyframe.lms.domain;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A text with gaps. The teacher writes the answers in double square brackets, with alternatives
 * separated by "|":   "Spring Boot runs on [[Java|the JVM]] and listens on port [[8080]]."
 * Students see the text with empty fields instead.
 * Answer format: one string per blank, in order. Not case-sensitive.
 * Partial credit: the share of blanks filled in correctly.
 */
@Entity
@DiscriminatorValue("FILL_IN_BLANKS")
public class FillInBlanksQuestion extends Question {

    private static final Pattern BLANK = Pattern.compile("\\[\\[(.+?)]]");

    /** The text with the answers in [[ ]]. Only teachers ever see this. */
    @Column(name = "blank_text", length = 4000)
    private String text;

    @Override
    public QuestionType getType() {
        return QuestionType.FILL_IN_BLANKS;
    }

    /** The text around the blanks: one more part than there are blanks. Safe to show to students. */
    public List<String> textParts() {
        return Arrays.asList(BLANK.split(text == null ? "" : text, -1));
    }

    /** For each blank, its accepted answers. */
    public List<List<String>> blanks() {
        List<List<String>> result = new ArrayList<>();
        Matcher m = BLANK.matcher(text == null ? "" : text);
        while (m.find()) {
            result.add(Arrays.stream(m.group(1).split("\\|")).map(Question::clean).filter(s -> !s.isEmpty()).toList());
        }
        return result;
    }

    @Override
    public void validate() {
        List<List<String>> blanks = blanks();
        if (blanks.isEmpty()) {
            throw new IllegalArgumentException("Mark at least one blank with [[answer]] in the text");
        }
        if (blanks.stream().anyMatch(List::isEmpty)) {
            throw new IllegalArgumentException("Every blank needs at least one accepted answer");
        }
    }

    @Override
    public double score(List<String> answer) {
        List<List<String>> blanks = blanks();
        if (answer == null || blanks.isEmpty()) {
            return 0;
        }
        int right = 0;
        for (int i = 0; i < blanks.size() && i < answer.size(); i++) {
            String given = clean(answer.get(i));
            if (blanks.get(i).stream().anyMatch(accepted -> accepted.equalsIgnoreCase(given))) {
                right++;
            }
        }
        return (double) right / blanks.size();
    }

    @Override
    public List<String> correctAnswer() {
        return blanks().stream().map(alternatives -> alternatives.get(0)).toList();
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
