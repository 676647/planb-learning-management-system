package fr.skyframe.lms;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** US3 through the REST API: a student hands in, the teacher grades and publishes. */
@SpringBootTest
@AutoConfigureMockMvc
class GradingApiTests {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper json;

    @Test
    void teacherGradesAndPublishes() throws Exception {
        long teacherId = postForId("/api/users", """
                {"email": "grading-teacher@hvl.no", "name": "Tina Teacher"}
                """);
        long studentId = postForId("/api/users", """
                {"email": "grading-student@hvl.no", "name": "Sam Student"}
                """);
        long courseId = postForId("/api/courses", """
                {"title": "DAT250", "teacherId": %d}
                """.formatted(teacherId));
        mvc.perform(post("/api/courses/{id}/enrollments", courseId).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "grading-student@hvl.no", "role": "STUDENT"}
                                """))
                .andExpect(status().isCreated());
        long moduleId = postForId("/api/courses/" + courseId + "/modules", """
                {"title": "Week 1"}
                """);
        long assignmentId = postForId("/api/modules/" + moduleId + "/assignments", """
                {"title": "Hand-in 1", "deadline": "2099-01-01T00:00:00Z", "maxPoints": 10}
                """);

        // Student hands in
        long submissionId = postForId("/api/contents/" + assignmentId + "/submissions", """
                {"studentId": %d, "answer": "https://git.app.uib.no/sam/hand-in-1"}
                """.formatted(studentId));

        // A student cannot grade
        mvc.perform(put("/api/submissions/{id}/grade", submissionId).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"teacherId": %d, "points": 10}
                                """.formatted(studentId)))
                .andExpect(status().isForbidden());

        // Teacher grades
        mvc.perform(put("/api/submissions/{id}/grade", submissionId).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"teacherId": %d, "points": 9, "feedback": "Clear and well structured"}
                                """.formatted(teacherId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.points").value(9.0));

        // The gradebook shows the result to the teacher
        mvc.perform(get("/api/courses/{id}/gradebook", courseId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].title").value("Hand-in 1"))
                .andExpect(jsonPath("$.students[0].entries[0].status").value("GRADED"))
                .andExpect(jsonPath("$.students[0].entries[0].points").value(9.0));

        // Before publishing the student does not see the grade ...
        mvc.perform(get("/api/courses/{c}/students/{s}/grades", courseId, studentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("SUBMITTED"))
                .andExpect(jsonPath("$[0].points").doesNotExist());

        // ... after publishing they do
        mvc.perform(put("/api/contents/{id}/grades-published", assignmentId).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"teacherId": %d, "published": true}
                                """.formatted(teacherId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gradesPublished").value(true));

        mvc.perform(get("/api/courses/{c}/students/{s}/grades", courseId, studentId))
                .andExpect(jsonPath("$[0].status").value("GRADED"))
                .andExpect(jsonPath("$[0].points").value(9.0))
                .andExpect(jsonPath("$[0].feedback").value("Clear and well structured"));
    }

    private long postForId(String url, String body) throws Exception {
        String response = mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(response).get("id").asLong();
    }
}
