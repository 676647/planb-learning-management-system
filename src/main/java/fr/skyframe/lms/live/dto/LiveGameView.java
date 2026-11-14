package fr.skyframe.lms.live.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import fr.skyframe.lms.live.LivePhase;
import fr.skyframe.lms.service.StudentQuestion;

import java.util.List;

/**
 * Everything the host's and players' screens need, sent over the WebSocket topic
 * /topic/live/{gameId} after every change, and returned by the REST calls.
 *
 * Never contains the correct answer while a question is open: `reveal` is only filled in REVEAL.
 * `me` is only set in REST answers (the topic is the same for everyone); players get their own
 * part on /user/queue/live.
 *
 * @param questionNumber  1-based; 0 in the lobby
 * @param remainingMillis time left for the current question (instead of a timestamp, so the
 *                        browser's clock doesn't need to match the server's)
 * @param players         names, only in the lobby
 * @param leaderboard     top 5 in REVEAL, everyone in FINISHED
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record LiveGameView(String gameId, String pin, Long quizId, String quizTitle, Long courseId, String hostName,
                           LivePhase phase, int questionNumber, int questionCount, StudentQuestion question,
                           int timeLimitSeconds, long remainingMillis, int playerCount, List<String> players,
                           int answeredCount, LiveReveal reveal, List<LeaderboardEntry> leaderboard, LiveMe me) {

    public LiveGameView withMe(LiveMe me) {
        return new LiveGameView(gameId, pin, quizId, quizTitle, courseId, hostName, phase, questionNumber,
                questionCount, question, timeLimitSeconds, remainingMillis, playerCount, players, answeredCount,
                reveal, leaderboard, me);
    }
}
