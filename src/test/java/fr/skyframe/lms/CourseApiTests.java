package fr.skyframe.lms;

import fr.skyframe.lms.support.ApiTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** US1 through the REST API, with logged-in users. */
@SpringBootTest
@AutoConfigureMockMvc
class CourseApiTests extends ApiTestSupport {

    @Test
    void teacherCreatesCourseAndEnrollsStudent() throws Exception {
        TestUser teacher = registerTeacher("course-teacher@hvl.no", "Tina Teacher");
        TestUser student = registerAndLogin("course-student@hvl.no", "Sam Student");

        // The logged-in user creates a course and becomes its teacher
        long courseId = create(teacher, "/api/courses", """
                {"title": "DAT250", "description": "Software technologies", "semester": "Autumn 2026"}
                """);

        String enroll = """
                {"email": "course-student@hvl.no", "role": "STUDENT"}
                """;
        postJson(teacher, "/api/courses/" + courseId + "/enrollments", enroll)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("STUDENT"));

        // Enrolling the same student twice is rejected
        postJson(teacher, "/api/courses/" + courseId + "/enrollments", enroll)
                .andExpect(status().isConflict());

        // The student sees the course among their enrollments
        getAs(student, "/api/users/me/enrollments")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].course.title").value("DAT250"))
                .andExpect(jsonPath("$[0].role").value("STUDENT"));

        // Both participants can see the participant list
        getAs(student, "/api/courses/" + courseId + "/enrollments")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void unknownCourseReturns404() throws Exception {
        TestUser user = registerAndLogin("course-someone@hvl.no", "Some One");
        getAs(user, "/api/courses/999999").andExpect(status().isNotFound());
        getAs(user, "/api/courses/999999/enrollments").andExpect(status().isNotFound());
    }

    @Test
    void invalidRegistrationReturns400() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "not-an-email", "name": "X", "password": "short"}
                                """))
                .andExpect(status().isBadRequest());
    }
}
