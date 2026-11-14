package fr.skyframe.lms.live;

/**
 * LOBBY → QUESTION → REVEAL → QUESTION → REVEAL → … → FINISHED
 *
 * - LOBBY:    players join with the PIN; the host starts when everyone is in.
 * - QUESTION: the question is shown with a timer; players answer.
 * - REVEAL:   time is up (or everyone answered): correct answer, answer counts and leaderboard.
 * - FINISHED: the podium. The result is saved.
 */
public enum LivePhase {
    LOBBY, QUESTION, REVEAL, FINISHED
}
