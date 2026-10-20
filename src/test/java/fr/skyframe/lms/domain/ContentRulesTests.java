package fr.skyframe.lms.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Plain unit tests of the time-based rules on content items. No Spring and no database,
 * so they run in milliseconds.
 */
class ContentRulesTests {

    private static final Instant MONDAY_8 = Instant.parse("2026-10-19T08:00:00Z");
    private static final Instant FRIDAY_23_59 = Instant.parse("2026-10-23T23:59:00Z");

    @Test
    void itemWithoutWindowIsAlwaysVisible() {
        Page page = new Page();
        assertThat(page.isVisibleAt(MONDAY_8)).isTrue();
    }

    @Test
    void itemIsHiddenBeforeVisibleFromAndVisibleFromThen() {
        Page page = new Page();
        page.setVisibleFrom(MONDAY_8);

        assertThat(page.isVisibleAt(MONDAY_8.minusSeconds(1))).isFalse();
        assertThat(page.isVisibleAt(MONDAY_8)).isTrue();
    }

    @Test
    void itemIsHiddenFromVisibleUntilOn() {
        Page page = new Page();
        page.setVisibleUntil(FRIDAY_23_59);

        assertThat(page.isVisibleAt(FRIDAY_23_59.minusSeconds(1))).isTrue();
        assertThat(page.isVisibleAt(FRIDAY_23_59)).isFalse();
    }

    @Test
    void submissionAfterDeadlineIsLateAndRejectedByDefault() {
        Assignment assignment = new Assignment();
        assignment.setDeadline(FRIDAY_23_59);

        assertThat(assignment.isLate(FRIDAY_23_59)).isFalse();
        assertThat(assignment.isLate(FRIDAY_23_59.plusSeconds(1))).isTrue();
        assertThat(assignment.acceptsSubmissionAt(FRIDAY_23_59.plusSeconds(1))).isFalse();
    }

    @Test
    void lateSubmissionIsAcceptedWhenTeacherAllowsIt() {
        Quiz quiz = new Quiz();
        quiz.setDeadline(FRIDAY_23_59);
        quiz.setLateSubmissionsAllowed(true);

        Instant late = FRIDAY_23_59.plusSeconds(3600);
        assertThat(quiz.isLate(late)).isTrue();          // still flagged as late ...
        assertThat(quiz.acceptsSubmissionAt(late)).isTrue(); // ... but accepted
    }

    @Test
    void withoutDeadlineNothingIsLate() {
        CodingExercise exercise = new CodingExercise();
        assertThat(exercise.isLate(Instant.parse("2099-01-01T00:00:00Z"))).isFalse();
    }
}
