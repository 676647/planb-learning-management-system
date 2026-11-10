package fr.skyframe.lms.live.dto;

/** Sent to one player on /user/queue/live when a question closes and when the game ends. */
public record LivePersonalUpdate(String gameId, LiveMe me) {
}
