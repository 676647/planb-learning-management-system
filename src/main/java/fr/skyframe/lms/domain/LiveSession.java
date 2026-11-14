package fr.skyframe.lms.domain;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * The saved result of a finished live quiz (the game itself runs in memory, see LiveGameService).
 * Live quizzes are just for fun: these results never go in the gradebook.
 */
@Entity
@Table(name = "live_sessions")
public class LiveSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "quiz_id")
    private Quiz quiz;

    @ManyToOne(optional = false)
    @JoinColumn(name = "host_id")
    private User host;

    @Column(nullable = false)
    private Instant startedAt;

    @Column(nullable = false)
    private Instant endedAt;

    /** How many questions were actually played (the host can end early). */
    private int questionsPlayed;

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("score DESC, id ASC") // the order they were ranked in (best first)
    private List<LiveSessionPlayer> players = new ArrayList<>();

    public void addPlayer(User user, int score, int correctAnswers, int rank) {
        players.add(new LiveSessionPlayer(this, user, score, correctAnswers, rank));
    }

    public Long getId() { return id; }
    public Quiz getQuiz() { return quiz; }
    public void setQuiz(Quiz quiz) { this.quiz = quiz; }
    public User getHost() { return host; }
    public void setHost(User host) { this.host = host; }
    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }
    public Instant getEndedAt() { return endedAt; }
    public void setEndedAt(Instant endedAt) { this.endedAt = endedAt; }
    public int getQuestionsPlayed() { return questionsPlayed; }
    public void setQuestionsPlayed(int questionsPlayed) { this.questionsPlayed = questionsPlayed; }
    public List<LiveSessionPlayer> getPlayers() { return players; }
}
