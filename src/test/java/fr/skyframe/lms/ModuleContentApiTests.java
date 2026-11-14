package fr.skyframe.lms;

import fr.skyframe.lms.support.ApiTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** A teacher builds a course structure through the REST API (US1). */
@SpringBootTest
@AutoConfigureMockMvc
class ModuleContentApiTests extends ApiTestSupport {

    @Test
    void teacherBuildsCourseStructure() throws Exception {
        TestUser teacher = registerTeacher("modules-teacher@hvl.no", "Tina Teacher");
        long courseId = courseWithStudents(teacher, "DAT250");

        long week1 = create(teacher, "/api/courses/" + courseId + "/modules", """
                {"title": "Week 1: Introduction"}
                """);
        long week2 = create(teacher, "/api/courses/" + courseId + "/modules", """
                {"title": "Week 2: Persistence"}
                """);

        // Week 2 can only be opened after Week 1 is completed
        putJson(teacher, "/api/modules/" + week2 + "/requires/" + week1, "")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requiredModuleIds[0]").value(week1));

        create(teacher, "/api/modules/" + week1 + "/pages", """
                {"title": "Welcome", "body": "# Welcome to DAT250"}
                """);
        create(teacher, "/api/modules/" + week1 + "/quizzes", """
                {"title": "Warm-up quiz", "maxAttempts": 3, "showHints": true}
                """);
        create(teacher, "/api/modules/" + week1 + "/assignments", """
                {"title": "Hand-in 1", "deadline": "2026-10-30T23:59:00Z", "lateSubmissionsAllowed": true}
                """);

        getAs(teacher, "/api/modules/" + week1 + "/contents")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].type").value("PAGE"))
                .andExpect(jsonPath("$[0].body").value("# Welcome to DAT250"))
                .andExpect(jsonPath("$[0].deadline").doesNotExist())
                .andExpect(jsonPath("$[1].type").value("QUIZ"))
                .andExpect(jsonPath("$[1].maxAttempts").value(3))
                .andExpect(jsonPath("$[2].type").value("ASSIGNMENT"))
                .andExpect(jsonPath("$[2].lateSubmissionsAllowed").value(true));

        getAs(teacher, "/api/courses/" + courseId + "/modules")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Week 1: Introduction"))
                .andExpect(jsonPath("$[1].title").value("Week 2: Persistence"));
    }

    @Test
    void quizWithZeroAttemptsIsRejected() throws Exception {
        TestUser teacher = registerTeacher("modules-teacher2@hvl.no", "Tom Teacher");
        long courseId = courseWithStudents(teacher, "DAT251");
        long moduleId = create(teacher, "/api/courses/" + courseId + "/modules", """
                {"title": "Only module"}
                """);

        postJson(teacher, "/api/modules/" + moduleId + "/quizzes", """
                {"title": "Impossible quiz", "maxAttempts": 0}
                """)
                .andExpect(status().isBadRequest());
    }

    @Test
    void contentsOfUnknownModuleReturn404() throws Exception {
        TestUser user = registerAndLogin("modules-someone@hvl.no", "Some One");
        getAs(user, "/api/modules/999999/contents").andExpect(status().isNotFound());
    }
}
