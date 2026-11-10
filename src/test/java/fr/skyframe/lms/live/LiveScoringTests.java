package fr.skyframe.lms.live;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class LiveScoringTests {

    private static final Duration LIMIT = Duration.ofSeconds(20);

    @Test
    void fasterCorrectAnswersEarnMore() {
        assertThat(LiveScoring.points(1.0, Duration.ZERO, LIMIT)).isEqualTo(1000);
        assertThat(LiveScoring.points(1.0, Duration.ofSeconds(10), LIMIT)).isEqualTo(750);
        assertThat(LiveScoring.points(1.0, LIMIT, LIMIT)).isEqualTo(500);
    }

    @Test
    void partlyCorrectAnswersEarnTheirShare() {
        assertThat(LiveScoring.points(0.5, Duration.ZERO, LIMIT)).isEqualTo(500);
        assertThat(LiveScoring.points(0.5, Duration.ofSeconds(10), LIMIT)).isEqualTo(375);
    }

    @Test
    void wrongAnswersEarnNothing() {
        assertThat(LiveScoring.points(0.0, Duration.ZERO, LIMIT)).isZero();
    }

    @Test
    void answersInTheGracePeriodCountAsLastMoment() {
        assertThat(LiveScoring.points(1.0, Duration.ofSeconds(21), LIMIT)).isEqualTo(500);
    }
}
