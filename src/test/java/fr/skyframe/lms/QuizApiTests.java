package fr.skyframe.lms;

import com.fasterxml.jackson.databind.JsonNode;
import fr.skyframe.lms.support.ApiTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Quizzes end to end: questions, the student view, attempts, automatic grading and the rules around them. */
@SpringBootTest
@AutoConfigureMockMvc
class QuizApiTests extends ApiTestSupport {

    /** A course with one student and a quiz (2 attempts, hints on) with one question of each type. */
    record QuizSetup(TestUser teacher, TestUser student, long courseId, long quizId,
                     long mcId, long orderingId, long matchingId, long blanksId) {
    }

    private QuizSetup quizWithAllQuestionTypes(String n) throws Exception {
        TestUser teacher = registerAndLogin("quiz-teacher" + n + "@hvl.no", "Tina Teacher");
        TestUser student = registerAndLogin("quiz-student" + n + "@hvl.no", "Sam Student");
        long courseId = courseWithStudents(teacher, "DAT250", student);
        long moduleId = create(teacher, "/api/courses/" + courseId + "/modules", """
                {"title": "Week 1"}
                """);
        long quizId = create(teacher, "/api/modules/" + moduleId + "/quizzes", """
                {"title": "Warm-up quiz", "maxAttempts": 2, "showHints": true}
                """);
        String q = "/api/contents/" + quizId + "/questions/";
        long mc = create(teacher, q + "multiple-choice", """
                {"prompt": "Which databases are relational?", "points": 2, "hint": "Think tables and SQL.",
                 "options": ["PostgreSQL", "MongoDB", "H2"], "correctOptions": [0, 2]}
                """);
        long ordering = create(teacher, q + "ordering", """
                {"prompt": "Order the CI stages", "points": 2, "items": ["Compile", "Test", "Package", "Deploy"]}
                """);
        long matching = create(teacher, q + "matching", """
                {"prompt": "Match technology and purpose", "points": 3,
                 "pairs": [{"left": "JPA", "right": "Persistence"}, {"left": "JWT", "right": "Security"},
                           {"left": "AMQP", "right": "Messaging"}]}
                """);
        long blanks = create(teacher, q + "fill-in-blanks", """
                {"prompt": "Fill in the blanks", "points": 2,
                 "text": "Spring Boot runs on [[Java|the JVM]] and listens on port [[8080]]."}
                """);
        return new QuizSetup(teacher, student, courseId, quizId, mc, ordering, matching, blanks);
    }

    @Test
    void teacherSeesSolutionsAndMaxPointsAreTheSumOfQuestionPoints() throws Exception {
        QuizSetup s = quizWithAllQuestionTypes("1");

        getAs(s.teacher(), "/api/contents/" + s.quizId() + "/questions")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4))
                .andExpect(jsonPath("$[0].correctOptions", contains(0, 2)))
                .andExpect(jsonPath("$[3].text", containsString("[[8080]]")));
        getAs(s.teacher(), "/api/contents/" + s.quizId())
                .andExpect(jsonPath("$.maxPoints").value(9));
    }

    @Test
    void studentsNeverReceiveTheSolutions() throws Exception {
        QuizSetup s = quizWithAllQuestionTypes("2");

        String body = getAs(s.student(), "/api/contents/" + s.quizId() + "/questions/student-view")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4))
                .andExpect(jsonPath("$[0].options", contains("PostgreSQL", "MongoDB", "H2")))
                .andExpect(jsonPath("$[0].multipleAnswers").value(true))
                .andExpect(jsonPath("$[3].textParts", contains("Spring Boot runs on ", " and listens on port ", ".")))
                .andExpect(jsonPath("$[3].blankCount").value(2))
                .andReturn().getResponse().getContentAsString();

        // No solution data anywhere in what students receive
        assertThat(body).doesNotContain("correctOptions", "pairs", "[[", "8080", "hint", "Think tables");
        JsonNode ordering = json.readTree(body).get(1).get("items");
        assertThat(ordering.toString()).isNotEqualTo("[\"Compile\",\"Test\",\"Package\",\"Deploy\"]"); // shuffled

        // The teacher-only endpoint is closed to students, and so is adding questions
        getAs(s.student(), "/api/contents/" + s.quizId() + "/questions").andExpect(status().isForbidden());
        postJson(s.student(), "/api/contents/" + s.quizId() + "/questions/ordering", """
                {"prompt": "x", "items": ["a", "b"]}
                """).andExpect(status().isForbidden());
    }

    @Test
    void attemptsAreGradedImmediatelyAndTheBestOneCounts() throws Exception {
        QuizSetup s = quizWithAllQuestionTypes("3");
        String attempts = "/api/contents/" + s.quizId() + "/attempts";

        // Attempt 1: MC wrong, ordering half right, matching right, one of two blanks right
        postJson(s.student(), attempts, """
                {"answers": {"%d": ["0"], "%d": ["Compile", "Package", "Test", "Deploy"],
                             "%d": ["Persistence", "Security", "Messaging"], "%d": ["Python", "8080"]}}
                """.formatted(s.mcId(), s.orderingId(), s.matchingId(), s.blanksId()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.attemptNumber").value(1))
                .andExpect(jsonPath("$.points").value(5.0))          // 0 + 1 + 3 + 1
                .andExpect(jsonPath("$.maxPoints").value(9))
                .andExpect(jsonPath("$.questions[0].correct").value(false))
                .andExpect(jsonPath("$.questions[0].hint").value("Think tables and SQL."))   // hints on
                .andExpect(jsonPath("$.questions[2].correct").value(true))
                .andExpect(jsonPath("$.questions[2].hint").doesNotExist())                  // only for wrong answers
                .andExpect(jsonPath("$.correctAnswersShown").value(false))                  // 1 attempt left
                .andExpect(jsonPath("$.questions[0].correctAnswer").doesNotExist());

        // Attempt 2: everything right
        postJson(s.student(), attempts, """
                {"answers": {"%d": ["2", "0"], "%d": ["Compile", "Test", "Package", "Deploy"],
                             "%d": ["Persistence", "Security", "Messaging"], "%d": ["the jvm", "8080"]}}
                """.formatted(s.mcId(), s.orderingId(), s.matchingId(), s.blanksId()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.attemptNumber").value(2))
                .andExpect(jsonPath("$.points").value(9.0))
                .andExpect(jsonPath("$.correctAnswersShown").value(true))                   // no attempts left
                .andExpect(jsonPath("$.questions[3].correctAnswer", contains("Java", "8080")));

        // No third attempt
        postJson(s.student(), attempts, """
                {"answers": {}}
                """).andExpect(status().isConflict());

        // The gradebook and the student's own overview count the best attempt
        getAs(s.teacher(), "/api/courses/" + s.courseId() + "/gradebook")
                .andExpect(jsonPath("$.students[0].entries[0].points").value(9.0));
        getAs(s.student(), "/api/courses/" + s.courseId() + "/my-grades")
                .andExpect(jsonPath("$[0].status").value("GRADED"))                          // visible right away
                .andExpect(jsonPath("$[0].points").value(9.0));
        getAs(s.student(), attempts + "/mine")
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].attemptNumber").value(2));
    }

    @Test
    void worseLaterAttemptDoesNotLowerTheGrade() throws Exception {
        QuizSetup s = quizWithAllQuestionTypes("4");
        String attempts = "/api/contents/" + s.quizId() + "/attempts";
        postJson(s.student(), attempts, """
                {"answers": {"%d": ["0", "2"]}}
                """.formatted(s.mcId())).andExpect(status().isCreated());     // 2 points
        postJson(s.student(), attempts, """
                {"answers": {}}
                """).andExpect(status().isCreated());                          // 0 points

        getAs(s.student(), "/api/courses/" + s.courseId() + "/my-grades")
                .andExpect(jsonPath("$[0].points").value(2.0));
    }

    @Test
    void questionsAreFrozenOnceStudentsHaveAnswered() throws Exception {
        QuizSetup s = quizWithAllQuestionTypes("5");
        postJson(s.student(), "/api/contents/" + s.quizId() + "/attempts", """
                {"answers": {}}
                """).andExpect(status().isCreated());

        postJson(s.teacher(), "/api/contents/" + s.quizId() + "/questions/ordering", """
                {"prompt": "Too late", "items": ["a", "b"]}
                """).andExpect(status().isConflict());
        deleteAs(s.teacher(), "/api/questions/" + s.mcId()).andExpect(status().isConflict());
    }

    @Test
    void invalidQuestionsAreRejected() throws Exception {
        QuizSetup s = quizWithAllQuestionTypes("6");
        postJson(s.teacher(), "/api/contents/" + s.quizId() + "/questions/fill-in-blanks", """
                {"prompt": "No blanks", "text": "There is nothing to fill in here."}
                """).andExpect(status().isBadRequest());
        postJson(s.teacher(), "/api/contents/" + s.quizId() + "/questions/multiple-choice", """
                {"prompt": "Nothing correct", "options": ["A", "B"], "correctOptions": []}
                """).andExpect(status().isBadRequest());
    }

    @Test
    void quizzesCantBeHandedInAsTextAndTeachersCantAttempt() throws Exception {
        QuizSetup s = quizWithAllQuestionTypes("7");
        postJson(s.student(), "/api/contents/" + s.quizId() + "/submissions", """
                {"answer": "my answers"}
                """).andExpect(status().isBadRequest());
        postJson(s.teacher(), "/api/contents/" + s.quizId() + "/attempts", """
                {"answers": {}}
                """).andExpect(status().isForbidden());
    }

    @Test
    void deletingTheQuizDeletesQuestionsAndAttempts() throws Exception {
        QuizSetup s = quizWithAllQuestionTypes("8");
        postJson(s.student(), "/api/contents/" + s.quizId() + "/attempts", """
                {"answers": {"%d": ["0", "2"]}}
                """.formatted(s.mcId())).andExpect(status().isCreated());

        deleteAs(s.teacher(), "/api/contents/" + s.quizId()).andExpect(status().isNoContent());
        getAs(s.teacher(), "/api/contents/" + s.quizId()).andExpect(status().isNotFound());
    }
}
