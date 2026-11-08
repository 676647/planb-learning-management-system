package fr.skyframe.lms.domain;

import jakarta.persistence.*;

import java.util.List;

/**
 * One question of a quiz. The subclasses are the question types; all are stored in one table
 * ("questions") with a "question_type" column, like the content items.
 *
 * Answers have the same shape for every type: a list of strings. What the strings mean depends on
 * the type (see each subclass), which keeps storing answers and the API simple.
 *
 * Each type grades itself: {@link #score(List)} returns how much of the question was answered
 * correctly, from 0.0 (nothing) to 1.0 (everything). The quiz multiplies that by the points.
 */
@Entity
@Table(name = "questions")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "question_type")
public abstract class Question {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz;

    /** Position within the quiz, starting at 0. */
    @Column(nullable = false)
    private int position;

    @Column(nullable = false, length = 2000)
    private String prompt;

    /** Points for a fully correct answer. */
    @Column(nullable = false)
    private int points = 1;

    /** Shown to students after a wrong answer, if the quiz has hints switched on. */
    @Column(length = 1000)
    private String hint;

    public abstract QuestionType getType();

    /** Throws IllegalArgumentException if the teacher's definition of the question is incomplete. */
    public abstract void validate();

    /** How much of the question the answer gets right: 0.0 to 1.0. A missing answer scores 0. */
    public abstract double score(List<String> answer);

    /** The correct answer, in the same format as a student's answer (shown after the last attempt). */
    public abstract List<String> correctAnswer();

    protected static String clean(String s) {
        return s == null ? "" : s.trim();
    }

    public Long getId() {
        return id;
    }

    public Quiz getQuiz() {
        return quiz;
    }

    public void setQuiz(Quiz quiz) {
        this.quiz = quiz;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public int getPoints() {
        return points;
    }

    public void setPoints(int points) {
        this.points = points;
    }

    public String getHint() {
        return hint;
    }

    public void setHint(String hint) {
        this.hint = hint;
    }
}
