package fr.skyframe.lms;

import com.fasterxml.jackson.databind.JsonNode;
import fr.skyframe.lms.config.DemoDataLoader;
import fr.skyframe.lms.repository.UserRepository;
import fr.skyframe.lms.support.ApiTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

/**
 * The test data in demo-data.json loads without errors, and only once.
 * (The other tests run without it; this one turns it on.)
 */
@SpringBootTest(properties = "lms.demo-data.enabled=true")
@AutoConfigureMockMvc
class DemoDataLoaderTests extends ApiTestSupport {

    @Autowired
    DemoDataLoader loader;

    @Autowired
    UserRepository users;

    private JsonNode body(String path, TestUser user) throws Exception {
        return json.readTree(getAs(user, path).andReturn().getResponse().getContentAsString());
    }

    @Test
    void usersCoursesAndContentAreThere() throws Exception {
        TestUser tina = login("tina@lms.local", "password123");
        TestUser anna = login("anna@lms.local", "password123");

        getAs(tina, "/api/users/me").andExpect(jsonPath("$.accountRole").value("TEACHER"));
        getAs(anna, "/api/users/me").andExpect(jsonPath("$.accountRole").value("STUDENT"));
        getAs(anna, "/api/users/me/enrollments")
                .andExpect(jsonPath("$[*].course.title", containsInAnyOrder(
                        "DAT250 Advanced Software Technologies", "DAT251 Modern Software Development Methods")))
                .andExpect(jsonPath("$[*].role", everyItem(is("STUDENT"))));

        JsonNode dat250 = body("/api/users/me/enrollments", tina).get(0).get("course");
        assertThat(dat250.get("title").asText()).startsWith("DAT250");
        long courseId = dat250.get("id").asLong();
        getAs(tina, "/api/courses/" + courseId + "/enrollments")
                .andExpect(jsonPath("$.length()").value(6)); // Tina, Lars and four students

        JsonNode modules = body("/api/courses/" + courseId + "/modules", tina);
        assertThat(modules).hasSize(3);
        long week1 = modules.get(0).get("id").asLong();
        long week2 = modules.get(1).get("id").asLong();

        // The warm-up quiz has all four question types; its points are the sum (1 + 1 + 1 + 2)
        JsonNode warmUp = body("/api/modules/" + week1 + "/contents", tina).get(2);
        assertThat(warmUp.get("title").asText()).isEqualTo("Warm-up quiz");
        assertThat(warmUp.get("maxPoints").asInt()).isEqualTo(5);
        getAs(tina, "/api/contents/" + warmUp.get("id").asLong() + "/questions")
                .andExpect(jsonPath("$[*].type", contains("MULTIPLE_CHOICE", "ORDERING", "MATCHING", "FILL_IN_BLANKS")));

        // The live quiz is hidden from students (it's meant to be hosted live)
        getAs(tina, "/api/modules/" + week2 + "/contents").andExpect(jsonPath("$.length()").value(3));
        getAs(anna, "/api/modules/" + week2 + "/contents")
                .andExpect(jsonPath("$[*].title", not(hasItem("Friday live quiz"))));
    }

    @Test
    void itIsOnlyInsertedOnce() throws Exception {
        long before = users.count();
        loader.run(null);
        assertThat(users.count()).isEqualTo(before);
    }
}
