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

/** A teacher builds a course structure through the REST API (US1). */
@SpringBootTest
@AutoConfigureMockMvc
class ModuleContentApiTests {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper json;

    @Test
    void teacherBuildsCourseStructure() throws Exception {
        long teacherId = postForId("/api/users", """
                {"email": "modules-teacher@hvl.no", "name": "Tina Teacher"}
                """);
        long courseId = postForId("/api/courses", """
                {"title": "DAT250", "semester": "Autumn 2026", "teacherId": %d}
                """.formatted(teacherId));

        long week1 = postForId("/api/courses/" + courseId + "/modules", """
                {"title": "Week 1: Introduction"}
                """);
        long week2 = postForId("/api/courses/" + courseId + "/modules", """
                {"title": "Week 2: Persistence"}
                """);

        // Week 2 can only be opened after Week 1 is completed
        mvc.perform(put("/api/modules/{id}/requires/{req}", week2, week1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requiredModuleIds[0]").value(week1));

        // Week 1 gets a page, a quiz and an assignment
        postForId("/api/modules/" + week1 + "/pages", """
                {"title": "Welcome", "body": "# Welcome to DAT250"}
                """);
        postForId("/api/modules/" + week1 + "/quizzes", """
                {"title": "Warm-up quiz", "maxAttempts": 3, "showHints": true}
                """);
        postForId("/api/modules/" + week1 + "/assignments", """
                {"title": "Hand-in 1", "deadline": "2026-10-30T23:59:00Z", "lateSubmissionsAllowed": true}
                """);

        mvc.perform(get("/api/modules/{id}/contents", week1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].type").value("PAGE"))
                .andExpect(jsonPath("$[0].body").value("# Welcome to DAT250"))
                .andExpect(jsonPath("$[0].deadline").doesNotExist())   // pages have no deadline
                .andExpect(jsonPath("$[1].type").value("QUIZ"))
                .andExpect(jsonPath("$[1].maxAttempts").value(3))
                .andExpect(jsonPath("$[2].type").value("ASSIGNMENT"))
                .andExpect(jsonPath("$[2].lateSubmissionsAllowed").value(true));

        mvc.perform(get("/api/courses/{id}/modules", courseId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Week 1: Introduction"))
                .andExpect(jsonPath("$[1].title").value("Week 2: Persistence"));
    }

    @Test
    void quizWithZeroAttemptsIsRejected() throws Exception {
        long teacherId = postForId("/api/users", """
                {"email": "modules-teacher2@hvl.no", "name": "Tom Teacher"}
                """);
        long courseId = postForId("/api/courses", """
                {"title": "DAT251", "teacherId": %d}
                """.formatted(teacherId));
        long moduleId = postForId("/api/courses/" + courseId + "/modules", """
                {"title": "Only module"}
                """);

        mvc.perform(post("/api/modules/{id}/quizzes", moduleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "Impossible quiz", "maxAttempts": 0}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void contentsOfUnknownModuleReturn404() throws Exception {
        mvc.perform(get("/api/modules/{id}/contents", 999_999))
                .andExpect(status().isNotFound());
    }

    /** POSTs JSON, expects 201 Created, and returns the "id" of the created resource. */
    private long postForId(String url, String body) throws Exception {
        String response = mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(response).get("id").asLong();
    }
}
