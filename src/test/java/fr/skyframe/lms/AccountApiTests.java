package fr.skyframe.lms;

import fr.skyframe.lms.support.ApiTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

import static org.hamcrest.Matchers.hasItems;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Only teachers (and admins) may create courses, and only an admin decides who is a teacher.
 * The admin account admin@test.local is created on startup from lms.admin.* in the test properties.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AccountApiTests extends ApiTestSupport {

    private static final String COURSE = """
            {"title": "My course", "semester": "Autumn 2026"}
            """;

    private TestUser admin() throws Exception {
        return login("admin@test.local", "admin-password");
    }

    private static String role(String role) {
        return """
                {"role": "%s"}
                """.formatted(role);
    }

    @Test
    void newAccountsAreStudentsAndCannotCreateCourses() throws Exception {
        TestUser user = registerAndLogin("acc-new1@hvl.no", "Nina New");
        getAs(user, "/api/users/me").andExpect(jsonPath("$.accountRole").value("STUDENT"));
        postJson(user, "/api/courses", COURSE).andExpect(status().isForbidden());
    }

    @Test
    void theStartupAdminCanCreateCourses() throws Exception {
        TestUser admin = admin();
        getAs(admin, "/api/users/me").andExpect(jsonPath("$.accountRole").value("ADMIN"));
        postJson(admin, "/api/courses", COURSE).andExpect(status().isCreated());
    }

    @Test
    void theAdminDecidesWhoIsATeacher() throws Exception {
        TestUser admin = admin();
        TestUser user = registerAndLogin("acc-tess2@hvl.no", "Tess Teacher");

        getAs(admin, "/api/admin/users")
                .andExpect(jsonPath("$[*].email", hasItems("admin@test.local", "acc-tess2@hvl.no")));
        putJson(admin, "/api/admin/users/" + user.id() + "/role", role("TEACHER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountRole").value("TEACHER"));

        // Takes effect immediately, with the same token
        getAs(user, "/api/users/me").andExpect(jsonPath("$.accountRole").value("TEACHER"));
        long courseId = create(user, "/api/courses", COURSE);

        // Taking the role away stops new courses, but the teacher keeps the course they have
        putJson(admin, "/api/admin/users/" + user.id() + "/role", role("STUDENT")).andExpect(status().isOk());
        postJson(user, "/api/courses", COURSE).andExpect(status().isForbidden());
        getAs(user, "/api/courses/" + courseId).andExpect(status().isOk());
    }

    @Test
    void onlyAdminsCanChangeRoles() throws Exception {
        TestUser teacher = registerTeacher("acc-teacher3@hvl.no", "Theo Teacher");
        TestUser student = registerAndLogin("acc-student3@hvl.no", "Stian Student");

        for (TestUser notAdmin : new TestUser[]{teacher, student}) {
            getAs(notAdmin, "/api/admin/users").andExpect(status().isForbidden());
            putJson(notAdmin, "/api/admin/users/" + student.id() + "/role", role("TEACHER"))
                    .andExpect(status().isForbidden());
        }
        getAs(student, "/api/users/me").andExpect(jsonPath("$.accountRole").value("STUDENT"));
    }

    @Test
    void anAdminCanMakeOtherAdminsButNotDemoteThemself() throws Exception {
        TestUser admin = admin();
        TestUser user = registerAndLogin("acc-ada4@hvl.no", "Ada Admin");

        putJson(admin, "/api/admin/users/" + user.id() + "/role", role("ADMIN")).andExpect(status().isOk());
        getAs(user, "/api/admin/users").andExpect(status().isOk());

        putJson(admin, "/api/admin/users/" + admin.id() + "/role", role("TEACHER")).andExpect(status().isConflict());
        putJson(admin, "/api/admin/users/" + user.id() + "/role", role("BOSS")).andExpect(status().isBadRequest());
        putJson(admin, "/api/admin/users/999999/role", role("TEACHER")).andExpect(status().isNotFound());
    }
}
