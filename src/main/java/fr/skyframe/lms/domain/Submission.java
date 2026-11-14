package fr.skyframe.lms.domain;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * Something a student handed in for a quiz, coding exercise or assignment.
 *
 * For group work, {@code group} is set and the submission counts for every member of the
 * group; {@code submittedBy} records which member actually handed it in.
 * Students may submit several times; the latest submission is the one that counts.
 */
@Entity
@Table(name = "submissions")
public class Submission {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "content_id", nullable = false)
    private GradedContent content;

    @ManyToOne(optional = false)
    @JoinColumn(name = "submitted_by_id", nullable = false)
    private User submittedBy;

    /** Set for group work, null for individual work. */
    @ManyToOne
    @JoinColumn(name = "group_id")
    private StudentGroup group;

    @Column(nullable = false)
    private Instant submittedAt;

    /** True if handed in after the deadline (only possible when late submissions are allowed). */
    @Column(nullable = false)
    private boolean late;

    /** The hand-in as text, or a link (e.g. to the student's Git repository). Optional if a file is attached. */
    @Column(length = 100_000)
    private String answer;

    /** An uploaded document (PDF, Word, …). The bytes are in MongoDB GridFS; this is only the reference. */
    @Embedded
    private FileReference file;

    /** True if this submission counts for the given student (as submitter or as group member). */
    public boolean countsFor(Long userId) {
        return group != null ? group.hasMember(userId) : submittedBy.getId().equals(userId);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public GradedContent getContent() {
        return content;
    }

    public void setContent(GradedContent content) {
        this.content = content;
    }

    public User getSubmittedBy() {
        return submittedBy;
    }

    public void setSubmittedBy(User submittedBy) {
        this.submittedBy = submittedBy;
    }

    public StudentGroup getGroup() {
        return group;
    }

    public void setGroup(StudentGroup group) {
        this.group = group;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(Instant submittedAt) {
        this.submittedAt = submittedAt;
    }

    public boolean isLate() {
        return late;
    }

    public void setLate(boolean late) {
        this.late = late;
    }

    public FileReference getFile() {
        return file;
    }

    public void setFile(FileReference file) {
        this.file = file;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }
}
