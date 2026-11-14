package fr.skyframe.lms.live.dto;

/**
 * @param fraction how correct, 0..1 (partly correct ordering, matching or blanks score in between)
 * @param points   points earned for this question (more for answering fast)
 */
public record LiveFeedback(int questionNumber, boolean answered, boolean correct, double fraction, int points) {
}
