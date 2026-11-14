package fr.skyframe.lms.live;

import fr.skyframe.lms.domain.Question;
import fr.skyframe.lms.service.StudentQuestion;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ScheduledFuture;

/**
 * The state of one running live quiz. Kept in memory only (a game lasts minutes, and many
 * small updates per second would be wasteful in the database); the final result is saved
 * as a LiveSession. Every change happens while holding the game's lock (synchronized on it).
 */
class LiveGame {

    /** One question as it is played: the entity (for scoring), what players see, and the time limit. */
    record LiveQuestion(Question question, StudentQuestion view, List<String> correctAnswer, int seconds) {
    }

    /** One player's answer to one question. */
    record PlayerAnswer(List<String> answer, double fraction, int points) {
    }

    static class Player {
        final Long userId;
        final String email;
        final String name;
        int score;
        int correctAnswers;
        final Map<Integer, PlayerAnswer> answers = new HashMap<>();

        Player(Long userId, String email, String name) {
            this.userId = userId;
            this.email = email;
            this.name = name;
        }
    }

    final String id;
    final String pin;
    final Long quizId;
    final String quizTitle;
    final Long courseId;
    final Long hostId;
    final String hostName;
    final List<LiveQuestion> questions;
    final Instant createdAt;
    /** In joining order. */
    final Map<Long, Player> players = new LinkedHashMap<>();

    LivePhase phase = LivePhase.LOBBY;
    /** Index of the current question; -1 in the lobby. */
    int index = -1;
    Instant questionStartedAt;
    Instant deadline;
    Instant endedAt;
    Instant lastActivity;
    /** Closes the current question when its time is up. */
    ScheduledFuture<?> timer;

    LiveGame(String id, String pin, Long quizId, String quizTitle, Long courseId, Long hostId, String hostName,
             List<LiveQuestion> questions, Instant now) {
        this.id = id;
        this.pin = pin;
        this.quizId = quizId;
        this.quizTitle = quizTitle;
        this.courseId = courseId;
        this.hostId = hostId;
        this.hostName = hostName;
        this.questions = questions;
        this.createdAt = now;
        this.lastActivity = now;
    }

    boolean isActive() {
        return phase != LivePhase.FINISHED;
    }

    LiveQuestion current() {
        return index >= 0 && index < questions.size() ? questions.get(index) : null;
    }

    int answeredCount() {
        return (int) players.values().stream().filter(p -> p.answers.containsKey(index)).count();
    }

    /** Highest score first; equal scores share a rank (1, 2, 2, 4). Ties listed by name. */
    List<Player> ranking() {
        return players.values().stream()
                .sorted(Comparator.comparingInt((Player p) -> p.score).reversed()
                        .thenComparing(p -> p.name, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    int rankOf(Player player) {
        return 1 + (int) players.values().stream().filter(p -> p.score > player.score).count();
    }

    void cancelTimer() {
        if (timer != null) {
            timer.cancel(false);
            timer = null;
        }
    }
}
