package fr.skyframe.lms;

import fr.skyframe.lms.support.ApiTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Domain events → notifications, end to end through the API. In tests the events are delivered
 * in-process after commit (lms.messaging.type=local) instead of through RabbitMQ; the
 * notification logic is the same.
 */
@SpringBootTest
@AutoConfigureMockMvc
class NotificationApiTests extends ApiTestSupport {

    @Test
    void enrolledUserIsNotified() throws Exception {
        TestUser teacher = registerAndLogin("notif-teacher1@hvl.no", "Tina Teacher");
        TestUser student = registerAndLogin("notif-student1@hvl.no", "Sam Student");
        courseWithStudents(teacher, "DAT250", student);

        getAs(student, "/api/notifications")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unread").value(1))
                .andExpect(jsonPath("$.items[0].message").value("You were added to 'DAT250' as a student."));
    }

    @Test
    void lateHandInNotifiesTeachersAndGivesStudentAReceipt() throws Exception {
        TestUser teacher = registerAndLogin("notif-teacher2@hvl.no", "Tina Teacher");
        TestUser student = registerAndLogin("notif-student2@hvl.no", "Sam Student");
        long courseId = courseWithStudents(teacher, "DAT250", student);
        long moduleId = create(teacher, "/api/courses/" + courseId + "/modules", """
                {"title": "Week 1"}
                """);
        // The deadline has already passed, but late hand-ins are accepted
        long assignmentId = create(teacher, "/api/modules/" + moduleId + "/assignments", """
                {"title": "Hand-in 1", "deadline": "2020-01-01T00:00:00Z", "lateSubmissionsAllowed": true}
                """);

        create(student, "/api/contents/" + assignmentId + "/submissions", """
                {"answer": "sorry, late"}
                """);

        getAs(teacher, "/api/notifications")
                .andExpect(jsonPath("$.items[0].message").value("Sam Student handed in 'Hand-in 1' after the deadline."))
                .andExpect(jsonPath("$.items[0].contentId").value(assignmentId));
        getAs(student, "/api/notifications")
                .andExpect(jsonPath("$.items[0].message", startsWith("You handed in 'Hand-in 1' (after the deadline")));
    }

    @Test
    void onTimeHandInDoesNotDisturbTeachers() throws Exception {
        TestUser teacher = registerAndLogin("notif-teacher3@hvl.no", "Tina Teacher");
        TestUser student = registerAndLogin("notif-student3@hvl.no", "Sam Student");
        long courseId = courseWithStudents(teacher, "DAT250", student);
        long moduleId = create(teacher, "/api/courses/" + courseId + "/modules", """
                {"title": "Week 1"}
                """);
        long assignmentId = create(teacher, "/api/modules/" + moduleId + "/assignments", """
                {"title": "Hand-in 1", "deadline": "2099-01-01T00:00:00Z"}
                """);

        create(student, "/api/contents/" + assignmentId + "/submissions", """
                {"answer": "on time"}
                """);

        getAs(teacher, "/api/notifications").andExpect(jsonPath("$.unread").value(0));
        getAs(student, "/api/notifications")
                .andExpect(jsonPath("$.items[0].message").value("You handed in 'Hand-in 1'."));
    }

    @Test
    void studentsAreToldWhenGradesArePublished() throws Exception {
        TestUser teacher = registerAndLogin("notif-teacher4@hvl.no", "Tina Teacher");
        TestUser anna = registerAndLogin("notif-anna4@hvl.no", "Anna");
        TestUser bob = registerAndLogin("notif-bob4@hvl.no", "Bob");
        long courseId = courseWithStudents(teacher, "DAT250", anna, bob);
        long moduleId = create(teacher, "/api/courses/" + courseId + "/modules", """
                {"title": "Week 1"}
                """);
        long assignmentId = create(teacher, "/api/modules/" + moduleId + "/assignments", """
                {"title": "Hand-in 1", "deadline": "2099-01-01T00:00:00Z", "maxPoints": 10}
                """);
        long submissionId = create(anna, "/api/contents/" + assignmentId + "/submissions", """
                {"answer": "Anna's work"}
                """);
        putJson(teacher, "/api/submissions/" + submissionId + "/grade", """
                {"points": 8}
                """).andExpect(status().isOk());

        // Grading alone tells nobody (grades are still hidden)
        getAs(anna, "/api/notifications")
                .andExpect(jsonPath("$.items[*].message", not(hasItem(containsString("now available")))));

        putJson(teacher, "/api/contents/" + assignmentId + "/grades-published", """
                {"published": true}
                """).andExpect(status().isOk());

        getAs(anna, "/api/notifications")
                .andExpect(jsonPath("$.items[0].message").value("Your grade for 'Hand-in 1' is now available."));
        // Bob handed nothing in, so he gets no grade message
        getAs(bob, "/api/notifications")
                .andExpect(jsonPath("$.items[*].message", not(hasItem(containsString("now available")))));
    }

    @Test
    void notificationsCanBeMarkedReadAndArePrivate() throws Exception {
        TestUser teacher = registerAndLogin("notif-teacher5@hvl.no", "Tina Teacher");
        TestUser student = registerAndLogin("notif-student5@hvl.no", "Sam Student");
        courseWithStudents(teacher, "DAT250", student);
        String body = getAs(student, "/api/notifications").andReturn().getResponse().getContentAsString();
        long notificationId = json.readTree(body).get("items").get(0).get("id").asLong();

        // Someone else's notification looks like it doesn't exist
        putJson(teacher, "/api/notifications/" + notificationId + "/read", "").andExpect(status().isNotFound());

        putJson(student, "/api/notifications/" + notificationId + "/read", "")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.read").value(true));
        getAs(student, "/api/notifications").andExpect(jsonPath("$.unread").value(0));
    }
}
