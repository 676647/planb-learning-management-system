package fr.skyframe.lms;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Exercises US1 end to end through the REST API, using the in-memory H2 database. */
@SpringBootTest
@AutoConfigureMockMvc
class CourseApiTests {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper json;

    @Test
    void teacherCreatesCourseAndEnrollsStudent() throws Exception {
        long teacherId = createUser("teacher@hvl.no", "Tina Teacher");
        createUser("student@hvl.no", "Sam Student");

        // Teacher creates a course and is automatically enrolled as TEACHER
        String courseJson = mvc.perform(post("/api/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "DAT250", "description": "Software technologies",
                                 "semester": "Autumn 2026", "teacherId": %d}
                                """.formatted(teacherId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("DAT250"))
                .andReturn().getResponse().getContentAsString();
        long courseId = json.readTree(courseJson).get("id").asLong();

        // Teacher invites a student by e-mail
        String enroll = """
                {"email": "student@hvl.no", "role": "STUDENT"}
                """;
        mvc.perform(post("/api/courses/{id}/enrollments", courseId)
                        .contentType(MediaType.APPLICATION_JSON).content(enroll))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("STUDENT"));

        // Enrolling the same student twice is rejected
        mvc.perform(post("/api/courses/{id}/enrollments", courseId)
                        .contentType(MediaType.APPLICATION_JSON).content(enroll))
                .andExpect(status().isConflict());

        // The course now has two participants
        mvc.perform(get("/api/courses/{id}/enrollments", courseId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void unknownCourseReturns404() throws Exception {
        mvc.perform(get("/api/courses/{id}", 999_999))
                .andExpect(status().isNotFound());
    }

    @Test
    void invalidEmailReturns400() throws Exception {
        mvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "not-an-email", "name": "X"}
                                """))
                .andExpect(status().isBadRequest());
    }

    private long createUser(String email, String name) throws Exception {
        String body = mvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "name": "%s"}
                                """.formatted(email, name)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        JsonNode node = json.readTree(body);
        return node.get("id").asLong();
    }
}
