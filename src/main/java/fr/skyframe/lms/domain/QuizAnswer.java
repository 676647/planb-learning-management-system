package fr.skyframe.lms.domain;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

/**
 * A student's answer to one question in one quiz attempt (a Submission), with the points it earned.
 * The result is stored at grading time, so an attempt's result stays as it was graded.
 */
@Entity
@Table(name = "quiz_answers")
public class QuizAnswer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "submission_id", nullable = false)
    private Submission submission;

    @ManyToOne(optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    /** The answer in the question type's format, stored as JSON text. */
    @Convert(converter = StringListJsonConverter.class)
    @Column(name = "answer_json", length = 10_000)
    private List<String> answer = new ArrayList<>();

    @Column(nullable = false)
    private double earnedPoints;

    /** True if the answer was completely right. */
    @Column(nullable = false)
    private boolean correct;

    public Long getId() {
        return id;
    }

    public Submission getSubmission() {
        return submission;
    }

    public void setSubmission(Submission submission) {
        this.submission = submission;
    }

    public Question getQuestion() {
        return question;
    }

    public void setQuestion(Question question) {
        this.question = question;
    }

    public List<String> getAnswer() {
        return answer;
    }

    public void setAnswer(List<String> answer) {
        this.answer = answer == null ? new ArrayList<>() : new ArrayList<>(answer);
    }

    public double getEarnedPoints() {
        return earnedPoints;
    }

    public void setEarnedPoints(double earnedPoints) {
        this.earnedPoints = earnedPoints;
    }

    public boolean isCorrect() {
        return correct;
    }

    public void setCorrect(boolean correct) {
        this.correct = correct;
    }
}
