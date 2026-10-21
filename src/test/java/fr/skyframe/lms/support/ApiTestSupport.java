package fr.skyframe.lms.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Helpers for API tests: register and log in users, and send requests with their token.
 * Test classes extend this and add @SpringBootTest @AutoConfigureMockMvc.
 *
 * All API tests share one database (and commit their data), so every test uses its own
 * e-mail addresses.
 */
public abstract class ApiTestSupport {

    protected static final String PASSWORD = "secret-password";

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected ObjectMapper json;

    /** A logged-in test user: the Authorization header value and the user's id. */
    public record TestUser(String auth, long id, String email) {
    }

    protected TestUser registerAndLogin(String email, String name) throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "name": "%s", "password": "%s"}
                                """.formatted(email, name, PASSWORD)))
                .andExpect(status().isCreated());
        String body = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "password": "%s"}
                                """.formatted(email, PASSWORD)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String auth = "Bearer " + json.readTree(body).get("accessToken").asText();
        String me = mvc.perform(get("/api/users/me").header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return new TestUser(auth, json.readTree(me).get("id").asLong(), email);
    }

    protected ResultActions postJson(TestUser user, String url, String body) throws Exception {
        return mvc.perform(post(url).header(HttpHeaders.AUTHORIZATION, user.auth())
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    protected ResultActions putJson(TestUser user, String url, String body) throws Exception {
        return mvc.perform(put(url).header(HttpHeaders.AUTHORIZATION, user.auth())
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    protected ResultActions getAs(TestUser user, String url) throws Exception {
        return mvc.perform(get(url).header(HttpHeaders.AUTHORIZATION, user.auth()));
    }

    protected ResultActions deleteAs(TestUser user, String url) throws Exception {
        return mvc.perform(delete(url).header(HttpHeaders.AUTHORIZATION, user.auth()));
    }

    /** POSTs JSON as the given user, expects 201 Created, and returns the new resource's id. */
    protected long create(TestUser user, String url, String body) throws Exception {
        String response = postJson(user, url, body)
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        JsonNode node = json.readTree(response);
        return node.get("id").asLong();
    }

    /** The teacher creates a course and enrolls the given students; returns the course id. */
    protected long courseWithStudents(TestUser teacher, String title, TestUser... students) throws Exception {
        long courseId = create(teacher, "/api/courses", """
                {"title": "%s", "semester": "Autumn 2026"}
                """.formatted(title));
        for (TestUser s : students) {
            postJson(teacher, "/api/courses/" + courseId + "/enrollments", """
                    {"email": "%s", "role": "STUDENT"}
                    """.formatted(s.email()))
                    .andExpect(status().isCreated());
        }
        return courseId;
    }
}
