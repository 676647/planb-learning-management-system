package fr.skyframe.lms;

import fr.skyframe.lms.support.ApiTestSupport;
import fr.skyframe.lms.support.MutableClock;
import fr.skyframe.lms.support.TestClockConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Live quizzes through the REST API: hosting, joining with the PIN, answering against the clock,
 * revealing, the leaderboard and the saved result. (LiveWebSocketTests checks the pushed messages.)
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestClockConfig.class)
class LiveQuizApiTests extends ApiTestSupport {

    @Autowired
    MutableClock clock;

    /** A course with two students and a quiz with a multiple-choice and an ordering question. */
    record Setup(TestUser teacher, TestUser anna, TestUser bob, long courseId, long quizId) {
    }

    private Setup setup(String n) throws Exception {
        TestUser teacher = registerTeacher("live-teacher" + n + "@hvl.no", "Tina Teacher");
        TestUser anna = registerAndLogin("live-anna" + n + "@hvl.no", "Anna");
        TestUser bob = registerAndLogin("live-bob" + n + "@hvl.no", "Bob");
        long courseId = courseWithStudents(teacher, "DAT250", anna, bob);
        long moduleId = create(teacher, "/api/courses/" + courseId + "/modules", """
                {"title": "Week 1"}
                """);
        long quizId = create(teacher, "/api/modules/" + moduleId + "/quizzes", """
                {"title": "Friday quiz"}
                """);
        create(teacher, "/api/contents/" + quizId + "/questions/multiple-choice", """
                {"prompt": "Which databases are relational?", "options": ["PostgreSQL", "MongoDB", "H2"],
                 "correctOptions": [0, 2]}
                """);
        create(teacher, "/api/contents/" + quizId + "/questions/ordering", """
                {"prompt": "Order the CI stages", "items": ["Compile", "Test", "Package", "Deploy"]}
                """);
        return new Setup(teacher, anna, bob, courseId, quizId);
    }

    private ResultActions host(Setup s) throws Exception {
        return postJson(s.teacher(), "/api/contents/" + s.quizId() + "/live", """
                {"secondsPerQuestion": 20}
                """);
    }

    private String text(ResultActions result, String field) throws Exception {
        return json.readTree(result.andReturn().getResponse().getContentAsString()).get(field).asText();
    }

    private ResultActions join(TestUser user, String pin) throws Exception {
        return postJson(user, "/api/live/join", """
                {"pin": "%s"}
                """.formatted(pin));
    }

    private ResultActions answer(TestUser user, String gameId, int question, String answer) throws Exception {
        return postJson(user, "/api/live/" + gameId + "/answers", """
                {"questionNumber": %d, "answer": %s}
                """.formatted(question, answer));
    }

    @Test
    void aWholeGame() throws Exception {
        Setup s = setup("1");
        ResultActions hosted = host(s)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.phase").value("LOBBY"))
                .andExpect(jsonPath("$.pin", matchesPattern("\\d{6}")))
                .andExpect(jsonPath("$.questionCount").value(2))
                .andExpect(jsonPath("$.me.host").value(true));
        String gameId = text(hosted, "gameId");
        String pin = text(hosted, "pin");

        // The course page shows that a quiz is running
        getAs(s.anna(), "/api/courses/" + s.courseId() + "/live")
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.pin").value(pin));

        join(s.anna(), pin).andExpect(status().isOk()).andExpect(jsonPath("$.players", contains("Anna")));
        join(s.bob(), " " + pin.substring(0, 3) + " " + pin.substring(3)) // spaces in the PIN are fine
                .andExpect(jsonPath("$.playerCount").value(2));
        join(s.anna(), pin).andExpect(jsonPath("$.playerCount").value(2)); // joining again changes nothing

        // Question 1: Anna answers right after 10 of 20 seconds, Bob doesn't answer
        postJson(s.teacher(), "/api/live/" + gameId + "/next", "{}")
                .andExpect(jsonPath("$.phase").value("QUESTION"))
                .andExpect(jsonPath("$.questionNumber").value(1))
                .andExpect(jsonPath("$.question.options", contains("PostgreSQL", "MongoDB", "H2")))
                .andExpect(jsonPath("$.remainingMillis").value(20_000))
                .andExpect(jsonPath("$.reveal").doesNotExist());
        clock.advance(Duration.ofSeconds(10));
        answer(s.anna(), gameId, 1, "[\"0\", \"2\"]")
                .andExpect(jsonPath("$.me.answeredCurrent").value(true))
                .andExpect(jsonPath("$.answeredCount").value(1))
                .andExpect(jsonPath("$.reveal").doesNotExist()); // no hints about the answer yet
        answer(s.anna(), gameId, 1, "[\"0\"]").andExpect(status().isConflict()); // only once

        postJson(s.teacher(), "/api/live/" + gameId + "/reveal", "{}")
                .andExpect(jsonPath("$.phase").value("REVEAL"))
                .andExpect(jsonPath("$.reveal.correctAnswer", contains("0", "2")))
                .andExpect(jsonPath("$.reveal.optionCounts", contains(1, 0, 1)))
                .andExpect(jsonPath("$.reveal.fullyCorrect").value(1))
                .andExpect(jsonPath("$.leaderboard[0].name").value("Anna"))
                .andExpect(jsonPath("$.leaderboard[0].score").value(750));
        getAs(s.bob(), "/api/live/" + gameId)
                .andExpect(jsonPath("$.me.lastAnswer.answered").value(false))
                .andExpect(jsonPath("$.me.score").value(0))
                .andExpect(jsonPath("$.me.rank").value(2));
        answer(s.bob(), gameId, 1, "[\"0\"]").andExpect(status().isConflict()); // closed

        // Question 2: when everyone has answered, the answer is shown without waiting
        postJson(s.teacher(), "/api/live/" + gameId + "/next", "{}")
                .andExpect(jsonPath("$.question.type").value("ORDERING"));
        answer(s.anna(), gameId, 2, "[\"Compile\", \"Test\", \"Package\", \"Deploy\"]")
                .andExpect(jsonPath("$.phase").value("QUESTION"));
        answer(s.bob(), gameId, 2, "[\"Compile\", \"Test\", \"Deploy\", \"Package\"]") // half right
                .andExpect(jsonPath("$.phase").value("REVEAL"))
                .andExpect(jsonPath("$.me.lastAnswer.points").value(500))
                .andExpect(jsonPath("$.me.lastAnswer.correct").value(false));

        // After the last question: the podium, and the result is saved
        postJson(s.teacher(), "/api/live/" + gameId + "/next", "{}")
                .andExpect(jsonPath("$.phase").value("FINISHED"))
                .andExpect(jsonPath("$.leaderboard[*].name", contains("Anna", "Bob")))
                .andExpect(jsonPath("$.leaderboard[*].score", contains(1750, 500)));
        getAs(s.anna(), "/api/courses/" + s.courseId() + "/live").andExpect(jsonPath("$.active").value(false));
        join(s.anna(), pin).andExpect(status().isNotFound());

        getAs(s.teacher(), "/api/contents/" + s.quizId() + "/live-sessions")
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].questionsPlayed").value(2))
                .andExpect(jsonPath("$[0].players[0].name").value("Anna"))
                .andExpect(jsonPath("$[0].players[0].rank").value(1))
                .andExpect(jsonPath("$[0].players[1].correctAnswers").value(0));

        // Live results don't count: the gradebook stays empty
        getAs(s.teacher(), "/api/courses/" + s.courseId() + "/gradebook")
                .andExpect(jsonPath("$.students[*].entries[*].status", everyItem(is("NOT_SUBMITTED"))))
                .andExpect(jsonPath("$.students[*].totalPoints", everyItem(is(0.0))));
    }

    @Test
    void onlyTheRightPeopleCanDoThings() throws Exception {
        Setup s = setup("2");
        TestUser outsider = registerAndLogin("live-outsider2@hvl.no", "Olga Outsider");

        postJson(s.anna(), "/api/contents/" + s.quizId() + "/live", "{}").andExpect(status().isForbidden());
        ResultActions hosted = host(s);
        String gameId = text(hosted, "gameId");
        String pin = text(hosted, "pin");

        host(s).andExpect(status().isConflict()); // one live quiz per course at a time
        join(outsider, pin).andExpect(status().isForbidden());
        join(s.teacher(), pin).andExpect(status().isConflict());
        join(s.anna(), "000000").andExpect(status().isNotFound());
        getAs(outsider, "/api/live/" + gameId).andExpect(status().isForbidden());
        getAs(outsider, "/api/courses/" + s.courseId() + "/live").andExpect(status().isForbidden());

        // The host needs players before starting
        postJson(s.teacher(), "/api/live/" + gameId + "/next", "{}").andExpect(status().isConflict());
        join(s.anna(), pin);
        getAs(s.bob(), "/api/live/" + gameId).andExpect(status().isForbidden()); // not joined
        postJson(s.anna(), "/api/live/" + gameId + "/next", "{}").andExpect(status().isForbidden());
        postJson(s.teacher(), "/api/live/" + gameId + "/next", "{}");
        postJson(s.teacher(), "/api/live/" + gameId + "/next", "{}").andExpect(status().isConflict());
        answer(s.teacher(), gameId, 1, "[\"0\"]").andExpect(status().isForbidden());
        answer(s.anna(), gameId, 2, "[\"0\"]").andExpect(status().isConflict()); // not that question yet

        // Too late
        clock.advance(Duration.ofSeconds(25));
        answer(s.anna(), gameId, 1, "[\"0\", \"2\"]").andExpect(status().isConflict());

        // Ending early saves what was played
        postJson(s.anna(), "/api/live/" + gameId + "/end", "{}").andExpect(status().isForbidden());
        postJson(s.teacher(), "/api/live/" + gameId + "/end", "{}").andExpect(jsonPath("$.phase").value("FINISHED"));
        getAs(s.teacher(), "/api/contents/" + s.quizId() + "/live-sessions")
                .andExpect(jsonPath("$[0].questionsPlayed").value(1))
                .andExpect(jsonPath("$[0].players[0].score").value(0));
        getAs(s.anna(), "/api/contents/" + s.quizId() + "/live-sessions").andExpect(status().isForbidden());

        // A new game can start now
        host(s).andExpect(status().isCreated());
    }

    @Test
    void malformedAnswersCountAsWrong() throws Exception {
        Setup s = setup("3");
        ResultActions hosted = host(s);
        String gameId = text(hosted, "gameId");
        join(s.anna(), text(hosted, "pin"));
        postJson(s.teacher(), "/api/live/" + gameId + "/next", "{}");

        answer(s.anna(), gameId, 1, "[\"seven\"]")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phase").value("REVEAL"))
                .andExpect(jsonPath("$.me.lastAnswer.points").value(0))
                .andExpect(jsonPath("$.reveal.optionCounts", contains(0, 0, 0)));
    }

    @Test
    void quizzesNeedQuestionsAndDeletingAQuizDeletesItsResults() throws Exception {
        Setup s = setup("4");
        long moduleId = json.readTree(getAs(s.teacher(), "/api/courses/" + s.courseId() + "/modules")
                .andReturn().getResponse().getContentAsString()).get(0).get("id").asLong();
        long empty = create(s.teacher(), "/api/modules/" + moduleId + "/quizzes", """
                {"title": "Empty quiz"}
                """);
        postJson(s.teacher(), "/api/contents/" + empty + "/live", "{}").andExpect(status().isConflict());
        postJson(s.teacher(), "/api/contents/" + s.quizId() + "/live", """
                {"secondsPerQuestion": 2}
                """).andExpect(status().isBadRequest());

        ResultActions hosted = host(s);
        String gameId = text(hosted, "gameId");
        join(s.anna(), text(hosted, "pin"));
        postJson(s.teacher(), "/api/live/" + gameId + "/next", "{}");
        postJson(s.teacher(), "/api/live/" + gameId + "/end", "{}");
        assertThat(getAs(s.teacher(), "/api/contents/" + s.quizId() + "/live-sessions")
                .andReturn().getResponse().getContentAsString()).contains("Anna");

        deleteAs(s.teacher(), "/api/contents/" + s.quizId()).andExpect(status().isNoContent());
    }
}
