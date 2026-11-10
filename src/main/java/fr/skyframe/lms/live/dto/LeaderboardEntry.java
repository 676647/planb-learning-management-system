package fr.skyframe.lms.live.dto;

public record LeaderboardEntry(int rank, Long userId, String name, int score, int correctAnswers) {
}
