package fr.skyframe.lms.live.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * The viewer's own part of the game.
 *
 * @param answeredCurrent whether I have answered the open question
 * @param lastAnswer      how my answer to the last closed question went (REVEAL and FINISHED)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record LiveMe(boolean host, boolean player, int score, int rank, boolean answeredCurrent,
                     LiveFeedback lastAnswer) {
}
