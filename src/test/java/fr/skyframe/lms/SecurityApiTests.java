package fr.skyframe.lms;

import fr.skyframe.lms.support.ApiTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Authentication (401) and per-course authorization (403) rules.
 * 401 = we don't know who you are; 403 = we know, but you're not allowed.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SecurityApiTests extends ApiTestSupport {

    @Test
    void requestWithoutTokenIsRejected() throws Exception {
        mvc.perform(get("/api/courses")).andExpect(status().isUnauthorized());
    }

    @Test
    void forgedTokenIsRejected() throws Exception {
        mvc.perform(get("/api/courses").header(HttpHeaders.AUTHORIZATION, "Bearer not.a.real-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void wrongPasswordIsRejected() throws Exception {
        registerAndLogin("sec-user1@hvl.no", "User One");
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "sec-user1@hvl.no", "password": "wrong-password"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void studentsCannotAdministerTheCourse() throws Exception {
        TestUser teacher = registerTeacher("sec-teacher2@hvl.no", "Tara Teacher");
        TestUser student = registerAndLogin("sec-student2@hvl.no", "Sid Student");
        registerAndLogin("sec-other2@hvl.no", "Olav Other");
        long courseId = courseWithStudents(teacher, "DAT250", student);

        postJson(student, "/api/courses/" + courseId + "/enrollments", """
                {"email": "sec-other2@hvl.no", "role": "STUDENT"}
                """).andExpect(status().isForbidden());
        postJson(student, "/api/courses/" + courseId + "/modules", """
                {"title": "Student's own module"}
                """).andExpect(status().isForbidden());
        deleteAs(student, "/api/courses/" + courseId).andExpect(status().isForbidden());
        getAs(student, "/api/courses/" + courseId + "/gradebook").andExpect(status().isForbidden());
    }

    @Test
    void outsidersCannotSeeInsideACourse() throws Exception {
        TestUser teacher = registerTeacher("sec-teacher3@hvl.no", "Tom Teacher");
        TestUser outsider = registerAndLogin("sec-outsider3@hvl.no", "Ola Outsider");
        long courseId = courseWithStudents(teacher, "DAT251");
        long moduleId = create(teacher, "/api/courses/" + courseId + "/modules", """
                {"title": "Week 1"}
                """);

        getAs(outsider, "/api/courses/" + courseId).andExpect(status().isOk()); // the catalogue is public
        getAs(outsider, "/api/courses/" + courseId + "/enrollments").andExpect(status().isForbidden());
        getAs(outsider, "/api/modules/" + moduleId + "/contents").andExpect(status().isForbidden());
    }

    @Test
    void teacherOfOneCourseIsOnlyAStudentElsewhere() throws Exception {
        TestUser anna = registerTeacher("sec-anna4@hvl.no", "Anna");
        TestUser bob = registerTeacher("sec-bob4@hvl.no", "Bob");
        long annasCourse = courseWithStudents(anna, "Anna's course", bob);
        long bobsCourse = courseWithStudents(bob, "Bob's course", anna);

        // Roles are per course: Bob teaches his own course but is a student in Anna's
        deleteAs(bob, "/api/courses/" + annasCourse).andExpect(status().isForbidden());
        getAs(bob, "/api/courses/" + bobsCourse + "/gradebook").andExpect(status().isOk());
    }

    @Test
    void studentsOnlySeeVisibleContent() throws Exception {
        TestUser teacher = registerTeacher("sec-teacher5@hvl.no", "Tina Teacher");
        TestUser student = registerAndLogin("sec-student5@hvl.no", "Sam Student");
        long courseId = courseWithStudents(teacher, "DAT250", student);
        long moduleId = create(teacher, "/api/courses/" + courseId + "/modules", """
                {"title": "Week 1"}
                """);
        create(teacher, "/api/modules/" + moduleId + "/pages", """
                {"title": "Published"}
                """);
        long hiddenId = create(teacher, "/api/modules/" + moduleId + "/pages", """
                {"title": "Next year", "visibleFrom": "2099-01-01T00:00:00Z"}
                """);

        getAs(teacher, "/api/modules/" + moduleId + "/contents")
                .andExpect(jsonPath("$.length()").value(2));
        // Even without ?visibleOnly=true, students only get visible items
        getAs(student, "/api/modules/" + moduleId + "/contents")
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("Published"));
        getAs(student, "/api/contents/" + hiddenId).andExpect(status().isNotFound());
    }

    @Test
    void studentsCannotSeeEachOthersWork() throws Exception {
        TestUser teacher = registerTeacher("sec-teacher6@hvl.no", "Tina Teacher");
        TestUser anna = registerAndLogin("sec-anna6@hvl.no", "Anna");
        TestUser bob = registerAndLogin("sec-bob6@hvl.no", "Bob");
        long courseId = courseWithStudents(teacher, "DAT250", anna, bob);
        long moduleId = create(teacher, "/api/courses/" + courseId + "/modules", """
                {"title": "Week 1"}
                """);
        long assignmentId = create(teacher, "/api/modules/" + moduleId + "/assignments", """
                {"title": "Hand-in 1", "deadline": "2099-01-01T00:00:00Z"}
                """);
        long annasSubmission = create(anna, "/api/contents/" + assignmentId + "/submissions", """
                {"answer": "Anna's report"}
                """);

        getAs(anna, "/api/submissions/" + annasSubmission).andExpect(status().isOk());
        getAs(bob, "/api/submissions/" + annasSubmission).andExpect(status().isForbidden());
        getAs(bob, "/api/contents/" + assignmentId + "/submissions").andExpect(status().isForbidden());
        getAs(bob, "/api/courses/" + courseId + "/students/" + anna.id() + "/grades")
                .andExpect(status().isForbidden());
    }

    @Test
    void studentsCanJoinGroupsOnlyThemselves() throws Exception {
        TestUser teacher = registerTeacher("sec-teacher7@hvl.no", "Tina Teacher");
        TestUser anna = registerAndLogin("sec-anna7@hvl.no", "Anna");
        TestUser bob = registerAndLogin("sec-bob7@hvl.no", "Bob");
        long courseId = courseWithStudents(teacher, "DAT250", anna, bob);
        long groupId = create(teacher, "/api/courses/" + courseId + "/groups", """
                {"name": "Group 1"}
                """);

        postJson(anna, "/api/groups/" + groupId + "/members", """
                {"email": "sec-anna7@hvl.no"}
                """).andExpect(status().isOk());
        postJson(anna, "/api/groups/" + groupId + "/members", """
                {"email": "sec-bob7@hvl.no"}
                """).andExpect(status().isForbidden());
    }

    @Test
    void usersCanOnlyLookUpPeopleTheyShareACourseWith() throws Exception {
        TestUser teacher = registerTeacher("sec-teacher-lookup@hvl.no", "Tara Teacher");
        TestUser student = registerAndLogin("sec-student-lookup@hvl.no", "Sid Student");
        TestUser outsider = registerAndLogin("sec-outsider-lookup@hvl.no", "Olav Outsider");
        courseWithStudents(teacher, "DAT250", student);

        getAs(student, "/api/users/" + student.id()).andExpect(status().isOk());
        getAs(student, "/api/users/" + teacher.id()).andExpect(status().isOk());
        getAs(teacher, "/api/users/" + student.id()).andExpect(status().isOk());
        getAs(outsider, "/api/users/" + student.id()).andExpect(status().isForbidden());
        getAs(student, "/api/users/" + outsider.id()).andExpect(status().isForbidden());
    }
}
