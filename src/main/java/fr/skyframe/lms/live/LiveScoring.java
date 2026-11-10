package fr.skyframe.lms.live;

import fr.skyframe.lms.domain.QuestionType;

import java.time.Duration;

/** Kahoot-style points: right and fast is best. */
public final class LiveScoring {

    /** Points for a fully correct answer given the instant the question appears. */
    public static final int MAX_POINTS = 1000;

    private LiveScoring() {
    }

    /**
     * Points for one answer. A correct answer gets between 500 (at the last moment) and 1000
     * (immediately) points; a partly correct one (ordering, matching, blanks) the same share of that.
     *
     * @param fraction how correct the answer is, 0..1 (Question.score)
     * @param elapsed  time from showing the question to the answer
     * @param limit    the question's time limit
     */
    public static int points(double fraction, Duration elapsed, Duration limit) {
        if (fraction <= 0) {
            return 0;
        }
        double used = Math.min(1.0, Math.max(0.0, (double) elapsed.toMillis() / limit.toMillis()));
        return (int) Math.round(Math.min(fraction, 1.0) * MAX_POINTS * (1 - used / 2));
    }

    /** Default time per question: more for types that take longer to answer. */
    public static int defaultSeconds(QuestionType type) {
        return switch (type) {
            case MULTIPLE_CHOICE -> 20;
            case ORDERING -> 30;
            case FILL_IN_BLANKS -> 40;
            case MATCHING -> 45;
        };
    }
}
