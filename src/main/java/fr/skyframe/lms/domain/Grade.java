package fr.skyframe.lms.domain;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * The grade for one submission: points and optional written feedback.
 * Assignments are graded manually by a teacher (gradedBy is set); quizzes and coding
 * exercises will later be graded automatically (gradedBy is then null).
 * Whether students can see the grade is decided per content item (GradedContent.gradesPublished).
 */
@Entity
@Table(name = "grades")
public class Grade {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "submission_id", nullable = false, unique = true)
    private Submission submission;

    @Column(nullable = false)
    private double points;

    @Column(length = 20_000)
    private String feedback;

    /** The teacher who graded; null for automatic grading. */
    @ManyToOne
    @JoinColumn(name = "graded_by_id")
    private User gradedBy;

    @Column(nullable = false)
    private Instant gradedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Submission getSubmission() {
        return submission;
    }

    public void setSubmission(Submission submission) {
        this.submission = submission;
    }

    public double getPoints() {
        return points;
    }

    public void setPoints(double points) {
        this.points = points;
    }

    public String getFeedback() {
        return feedback;
    }

    public void setFeedback(String feedback) {
        this.feedback = feedback;
    }

    public User getGradedBy() {
        return gradedBy;
    }

    public void setGradedBy(User gradedBy) {
        this.gradedBy = gradedBy;
    }

    public Instant getGradedAt() {
        return gradedAt;
    }

    public void setGradedAt(Instant gradedAt) {
        this.gradedAt = gradedAt;
    }
}
