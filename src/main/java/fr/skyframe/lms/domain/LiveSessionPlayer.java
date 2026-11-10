package fr.skyframe.lms.domain;

import jakarta.persistence.*;

/** One player's final score in a finished live quiz. */
@Entity
@Table(name = "live_session_players")
public class LiveSessionPlayer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "session_id")
    private LiveSession session;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    private int score;

    private int correctAnswers;

    /** 1 = winner. Players with the same score share a rank. */
    @Column(name = "player_rank")
    private int rank;

    protected LiveSessionPlayer() {
    }

    LiveSessionPlayer(LiveSession session, User user, int score, int correctAnswers, int rank) {
        this.session = session;
        this.user = user;
        this.score = score;
        this.correctAnswers = correctAnswers;
        this.rank = rank;
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public int getScore() { return score; }
    public int getCorrectAnswers() { return correctAnswers; }
    public int getRank() { return rank; }
}
