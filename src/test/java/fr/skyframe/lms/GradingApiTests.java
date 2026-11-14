package fr.skyframe.lms;

import fr.skyframe.lms.support.ApiTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** US3 through the REST API: a student hands in, the teacher grades and publishes. */
@SpringBootTest
@AutoConfigureMockMvc
class GradingApiTests extends ApiTestSupport {

    @Test
    void teacherGradesAndPublishes() throws Exception {
        TestUser teacher = registerTeacher("grading-teacher@hvl.no", "Tina Teacher");
        TestUser student = registerAndLogin("grading-student@hvl.no", "Sam Student");
        long courseId = courseWithStudents(teacher, "DAT250", student);
        long moduleId = create(teacher, "/api/courses/" + courseId + "/modules", """
                {"title": "Week 1"}
                """);
        long assignmentId = create(teacher, "/api/modules/" + moduleId + "/assignments", """
                {"title": "Hand-in 1", "deadline": "2099-01-01T00:00:00Z", "maxPoints": 10}
                """);

        // The student hands in; the submitter is taken from the token
        long submissionId = create(student, "/api/contents/" + assignmentId + "/submissions", """
                {"answer": "https://git.app.uib.no/sam/hand-in-1"}
                """);

        // A student cannot grade, not even their own work
        putJson(student, "/api/submissions/" + submissionId + "/grade", """
                {"points": 10}
                """)
                .andExpect(status().isForbidden());

        putJson(teacher, "/api/submissions/" + submissionId + "/grade", """
                {"points": 9, "feedback": "Clear and well structured"}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.points").value(9.0));

        // The grading queue shows the submission together with its grade
        getAs(teacher, "/api/contents/" + assignmentId + "/submissions")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].submission.submittedBy.name").value("Sam Student"))
                .andExpect(jsonPath("$[0].grade.points").value(9.0));

        getAs(teacher, "/api/courses/" + courseId + "/gradebook")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].title").value("Hand-in 1"))
                .andExpect(jsonPath("$.students[0].entries[0].status").value("GRADED"))
                .andExpect(jsonPath("$.students[0].entries[0].points").value(9.0));

        // Before publishing the student does not see the grade ...
        getAs(student, "/api/courses/" + courseId + "/my-grades")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("SUBMITTED"))
                .andExpect(jsonPath("$[0].points").doesNotExist());
        getAs(student, "/api/submissions/" + submissionId + "/grade")
                .andExpect(status().isForbidden());

        // ... after publishing they do
        putJson(teacher, "/api/contents/" + assignmentId + "/grades-published", """
                {"published": true}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gradesPublished").value(true));

        getAs(student, "/api/courses/" + courseId + "/my-grades")
                .andExpect(jsonPath("$[0].status").value("GRADED"))
                .andExpect(jsonPath("$[0].points").value(9.0))
                .andExpect(jsonPath("$[0].feedback").value("Clear and well structured"));
        getAs(student, "/api/submissions/" + submissionId + "/grade")
                .andExpect(status().isOk());
    }
}
