package fr.skyframe.lms;

import fr.skyframe.lms.domain.Course;
import fr.skyframe.lms.domain.Enrollment;
import fr.skyframe.lms.domain.Role;
import fr.skyframe.lms.domain.StudentGroup;
import fr.skyframe.lms.domain.User;
import fr.skyframe.lms.service.ConflictException;
import fr.skyframe.lms.service.CourseService;
import fr.skyframe.lms.service.GroupService;
import fr.skyframe.lms.service.NotFoundException;
import fr.skyframe.lms.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests the business rules in the service layer directly (no HTTP involved).
 * @Transactional rolls the database back after each test, so tests don't affect each other.
 */
@SpringBootTest
@Transactional
class CourseServiceTests {

    @Autowired
    UserService userService;

    @Autowired
    CourseService courseService;

    @Autowired
    GroupService groupService;

    @Test
    void creatorBecomesTeacherOfNewCourse() {
        User teacher = userService.register("teacher@hvl.no", "Tina Teacher", "secret-password");

        Course course = courseService.create(teacher.getId(), "DAT250", "Software technologies", "Autumn 2026");

        List<Enrollment> participants = courseService.participants(course.getId());
        assertThat(participants).hasSize(1);
        assertThat(participants.get(0).getUser().getEmail()).isEqualTo("teacher@hvl.no");
        assertThat(participants.get(0).getRole()).isEqualTo(Role.TEACHER);
    }

    @Test
    void studentCanBeEnrolledByEmail() {
        User teacher = userService.register("teacher@hvl.no", "Tina Teacher", "secret-password");
        User student = userService.register("student@hvl.no", "Sam Student", "secret-password");
        Course course = courseService.create(teacher.getId(), "DAT250", null, "Autumn 2026");

        courseService.enroll(course.getId(), "student@hvl.no", Role.STUDENT);

        List<Enrollment> studentsCourses = courseService.coursesOfUser(student.getId());
        assertThat(studentsCourses).hasSize(1);
        assertThat(studentsCourses.get(0).getCourse().getTitle()).isEqualTo("DAT250");
        assertThat(studentsCourses.get(0).getRole()).isEqualTo(Role.STUDENT);
    }

    @Test
    void sameUserCannotBeEnrolledTwice() {
        User teacher = userService.register("teacher@hvl.no", "Tina Teacher", "secret-password");
        userService.register("student@hvl.no", "Sam Student", "secret-password");
        Course course = courseService.create(teacher.getId(), "DAT250", null, null);
        courseService.enroll(course.getId(), "student@hvl.no", Role.STUDENT);

        assertThatThrownBy(() -> courseService.enroll(course.getId(), "student@hvl.no", Role.STUDENT))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void emailsAreUniqueAndCaseInsensitive() {
        userService.register("Teacher@HVL.no", "Tina Teacher", "secret-password");

        assertThatThrownBy(() -> userService.register("teacher@hvl.no", "Someone Else", "secret-password"))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void courseCanBeUpdated() {
        User teacher = userService.register("teacher@hvl.no", "Tina Teacher", "secret-password");
        Course course = courseService.create(teacher.getId(), "DAT250", null, "Autumn 2026");

        courseService.update(course.getId(), "DAT250 Software Technologies", "Updated", "Autumn 2026");

        assertThat(courseService.get(course.getId()).getTitle()).isEqualTo("DAT250 Software Technologies");
    }

    @Test
    void deletingCourseRemovesItAndItsEnrollments() {
        User teacher = userService.register("teacher@hvl.no", "Tina Teacher", "secret-password");
        Course course = courseService.create(teacher.getId(), "DAT250", null, null);

        courseService.delete(course.getId());

        assertThatThrownBy(() -> courseService.get(course.getId()))
                .isInstanceOf(NotFoundException.class);
        assertThat(courseService.coursesOfUser(teacher.getId())).isEmpty();
    }

    @Test
    void enrollingUnknownUserFails() {
        User teacher = userService.register("teacher@hvl.no", "Tina Teacher", "secret-password");
        Course course = courseService.create(teacher.getId(), "DAT250", null, null);

        assertThatThrownBy(() -> courseService.enroll(course.getId(), "nobody@hvl.no", Role.STUDENT))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void lastTeacherCannotLeaveTheCourse() {
        User teacher = userService.register("teacher@hvl.no", "Tina Teacher", "secret-password");
        Course course = courseService.create(teacher.getId(), "DAT250", null, "Autumn 2026");

        assertThatThrownBy(() -> courseService.unenroll(course.getId(), teacher.getId()))
                .isInstanceOf(ConflictException.class);
        assertThat(courseService.participants(course.getId())).hasSize(1);
    }

    @Test
    void teacherCanLeaveWhenAnotherTeacherRemains() {
        User tina = userService.register("tina@hvl.no", "Tina Teacher", "secret-password");
        userService.register("lars@hvl.no", "Lars Lektor", "secret-password");
        Course course = courseService.create(tina.getId(), "DAT250", null, "Autumn 2026");
        courseService.enroll(course.getId(), "lars@hvl.no", Role.TEACHER);

        courseService.unenroll(course.getId(), tina.getId());

        List<Enrollment> participants = courseService.participants(course.getId());
        assertThat(participants).hasSize(1);
        assertThat(participants.get(0).getUser().getEmail()).isEqualTo("lars@hvl.no");
    }

    @Test
    void leavingTheCourseAlsoLeavesTheGroup() {
        User teacher = userService.register("teacher@hvl.no", "Tina Teacher", "secret-password");
        User student = userService.register("student@hvl.no", "Sam Student", "secret-password");
        Course course = courseService.create(teacher.getId(), "DAT250", null, "Autumn 2026");
        courseService.enroll(course.getId(), "student@hvl.no", Role.STUDENT);
        StudentGroup group = groupService.create(course.getId(), "Group 1");
        groupService.addMember(group.getId(), "student@hvl.no");

        courseService.unenroll(course.getId(), student.getId());

        assertThat(group.hasMember(student.getId())).isFalse();
        assertThat(groupService.groupOf(course.getId(), student.getId())).isEmpty();
    }
}
