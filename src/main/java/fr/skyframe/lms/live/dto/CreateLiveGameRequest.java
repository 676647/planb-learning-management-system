package fr.skyframe.lms.live.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * @param secondsPerQuestion the same time for every question; leave out for defaults per question type
 *                           (20 s multiple choice, 30 s ordering, 40 s blanks, 45 s matching)
 */
public record CreateLiveGameRequest(@Min(5) @Max(300) Integer secondsPerQuestion) {
}
