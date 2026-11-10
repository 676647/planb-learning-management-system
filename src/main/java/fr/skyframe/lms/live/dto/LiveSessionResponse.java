package fr.skyframe.lms.live.dto;

import java.time.Instant;
import java.util.List;

/** A finished live quiz, for the teacher to look back at. */
public record LiveSessionResponse(Long id, Instant startedAt, Instant endedAt, int questionsPlayed, String hostName,
                                  List<LeaderboardEntry> players) {
}
