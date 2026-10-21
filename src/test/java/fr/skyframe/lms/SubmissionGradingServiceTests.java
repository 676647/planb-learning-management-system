package fr.skyframe.lms;

import fr.skyframe.lms.domain.*;
import fr.skyframe.lms.service.*;
import fr.skyframe.lms.support.MutableClock;
import fr.skyframe.lms.support.TestClockConfig;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * US3 rules: submitting, deadlines, groups, grading, publishing and the gradebook.
 * Uses a MutableClock so the tests control what "now" is.
 */
@SpringBootTest
@Import(TestClockConfig.class)
@Transactional
class SubmissionGradingServiceTests {

    static final Instant MONDAY_8 = Instant.parse("2026-10-19T08:00:00Z");
    static final Instant FRIDAY_23_59 = Instant.parse("2026-10-23T23:59:00Z");

    @Autowired MutableClock clock;
    @Autowired UserService userService;
    @Autowired CourseService courseService;
    @Autowired ModuleService moduleService;
    @Autowired ContentService contentService;
    @Autowired GroupService groupService;
    @Autowired SubmissionService submissionService;
    @Autowired GradingService gradingService;
    @Autowired EntityManager entityManager;

    User teacher, anna, bob, carl;
    Course course;
    CourseModule week1;

    @BeforeEach
    void setUp() {
        clock.setInstant(MONDAY_8);
        teacher = userService.register("teacher@hvl.no", "Tina Teacher", "secret-password");
        anna = userService.register("anna@hvl.no", "Anna", "secret-password");
        bob = userService.register("bob@hvl.no", "Bob", "secret-password");
        carl = userService.register("carl@hvl.no", "Carl", "secret-password");
        course = courseService.create(teacher.getId(), "DAT250", null, "Autumn 2026");
        for (User s : List.of(anna, bob, carl)) {
            courseService.enroll(course.getId(), s.getEmail(), Role.STUDENT);
        }
        week1 = moduleService.create(course.getId(), "Week 1");
    }

    // --- submitting ---

    @Test
    void submissionBeforeDeadlineIsOnTime() {
        Assignment a = assignment("Hand-in 1", FRIDAY_23_59, false, false);

        Submission s = submissionService.submit(a.getId(), anna.getId(), "my report");

        assertThat(s.isLate()).isFalse();
        assertThat(s.getSubmittedAt()).isEqualTo(MONDAY_8);
    }

    @Test
    void lateSubmissionIsRejectedByDefault() {
        Assignment a = assignment("Hand-in 1", FRIDAY_23_59, false, false);
        clock.setInstant(FRIDAY_23_59.plusSeconds(60));

        assertThatThrownBy(() -> submissionService.submit(a.getId(), anna.getId(), "too late"))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void lateSubmissionIsAcceptedAndFlaggedWhenAllowed() {
        Assignment a = assignment("Hand-in 1", FRIDAY_23_59, true, false);
        clock.setInstant(FRIDAY_23_59.plus(Duration.ofHours(2)));

        Submission s = submissionService.submit(a.getId(), anna.getId(), "sorry, late");

        assertThat(s.isLate()).isTrue();
    }

    @Test
    void teachersCannotSubmit() {
        Assignment a = assignment("Hand-in 1", FRIDAY_23_59, false, false);

        assertThatThrownBy(() -> submissionService.submit(a.getId(), teacher.getId(), "x"))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void cannotSubmitBeforeItemIsVisible() {
        Assignment a = assignment("Hand-in 2", FRIDAY_23_59, false, false);
        a.setVisibleFrom(MONDAY_8.plus(Duration.ofDays(1)));

        assertThatThrownBy(() -> submissionService.submit(a.getId(), anna.getId(), "x"))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void quizAttemptsAreLimited() {
        Quiz quiz = new Quiz();
        quiz.setTitle("Warm-up");
        quiz.setMaxAttempts(2);
        quiz = contentService.add(week1.getId(), quiz);

        submissionService.submit(quiz.getId(), anna.getId(), "attempt 1");
        submissionService.submit(quiz.getId(), anna.getId(), "attempt 2");
        Long quizId = quiz.getId();

        assertThatThrownBy(() -> submissionService.submit(quizId, anna.getId(), "attempt 3"))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void pagesCannotBeSubmitted() {
        Page page = new Page();
        page.setTitle("Reading");
        Page saved = contentService.add(week1.getId(), page);

        assertThatThrownBy(() -> submissionService.submit(saved.getId(), anna.getId(), "x"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void groupWorkRequiresAGroup() {
        Assignment a = assignment("Group project", FRIDAY_23_59, false, true);

        assertThatThrownBy(() -> submissionService.submit(a.getId(), anna.getId(), "x"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void studentCanOnlyBeInOneGroupPerCourse() {
        StudentGroup g1 = groupService.create(course.getId(), "Group 1");
        StudentGroup g2 = groupService.create(course.getId(), "Group 2");
        groupService.addMember(g1.getId(), "anna@hvl.no");

        assertThatThrownBy(() -> groupService.addMember(g2.getId(), "anna@hvl.no"))
                .isInstanceOf(ConflictException.class);
    }

    // --- grading ---

    @Test
    void groupSubmissionAndGradeApplyToAllMembers() {
        Assignment project = assignment("Group project", FRIDAY_23_59, false, true);
        StudentGroup group = groupService.create(course.getId(), "Group 1");
        groupService.addMember(group.getId(), "anna@hvl.no");
        groupService.addMember(group.getId(), "bob@hvl.no");

        Submission s = submissionService.submit(project.getId(), anna.getId(), "our repo");
        gradingService.grade(s.getId(), teacher.getId(), 8, "Nice teamwork");

        Gradebook book = gradingService.gradebook(course.getId());
        assertThat(entry(book, anna, project).status()).isEqualTo(Gradebook.Status.GRADED);
        assertThat(entry(book, bob, project).status()).isEqualTo(Gradebook.Status.GRADED);
        assertThat(entry(book, bob, project).points()).isEqualTo(8.0);
        assertThat(entry(book, carl, project).status()).isEqualTo(Gradebook.Status.NOT_SUBMITTED);
    }

    @Test
    void onlyTeachersCanGrade() {
        Assignment a = assignment("Hand-in 1", FRIDAY_23_59, false, false);
        Submission s = submissionService.submit(a.getId(), anna.getId(), "report");

        assertThatThrownBy(() -> gradingService.grade(s.getId(), bob.getId(), 10, "I'd give it full marks"))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void pointsMustBeWithinRange() {
        Assignment a = assignment("Hand-in 1", FRIDAY_23_59, false, false); // maxPoints = 10
        Submission s = submissionService.submit(a.getId(), anna.getId(), "report");

        assertThatThrownBy(() -> gradingService.grade(s.getId(), teacher.getId(), 11, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> gradingService.grade(s.getId(), teacher.getId(), -1, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void regradingChangesTheExistingGrade() {
        Assignment a = assignment("Hand-in 1", FRIDAY_23_59, false, false);
        Submission s = submissionService.submit(a.getId(), anna.getId(), "report");

        Grade first = gradingService.grade(s.getId(), teacher.getId(), 5, "OK");
        Grade second = gradingService.grade(s.getId(), teacher.getId(), 7, "Better after a second look");

        assertThat(second.getId()).isEqualTo(first.getId());
        assertThat(gradingService.gradeOf(s.getId()).orElseThrow().getPoints()).isEqualTo(7.0);
    }

    @Test
    void studentsSeeGradesOnlyAfterPublishing() {
        Assignment a = assignment("Hand-in 1", FRIDAY_23_59, false, false);
        Submission s = submissionService.submit(a.getId(), anna.getId(), "report");
        gradingService.grade(s.getId(), teacher.getId(), 9, "Well done");

        StudentGrade before = gradingService.gradesOfStudent(course.getId(), anna.getId()).get(0);
        assertThat(before.status()).isEqualTo(Gradebook.Status.SUBMITTED);
        assertThat(before.points()).isNull();
        assertThat(before.feedback()).isNull();

        gradingService.setGradesPublished(a.getId(), teacher.getId(), true);

        StudentGrade after = gradingService.gradesOfStudent(course.getId(), anna.getId()).get(0);
        assertThat(after.status()).isEqualTo(Gradebook.Status.GRADED);
        assertThat(after.points()).isEqualTo(9.0);
        assertThat(after.feedback()).isEqualTo("Well done");
    }

    @Test
    void latestSubmissionIsTheOneThatCounts() {
        Assignment a = assignment("Hand-in 1", FRIDAY_23_59, false, false);
        Submission first = submissionService.submit(a.getId(), anna.getId(), "draft");
        gradingService.grade(first.getId(), teacher.getId(), 4, "Draft graded");
        clock.advance(Duration.ofDays(1));
        Submission second = submissionService.submit(a.getId(), anna.getId(), "final version");

        Gradebook.Entry e = entry(gradingService.gradebook(course.getId()), anna, a);

        assertThat(e.submissionId()).isEqualTo(second.getId());
        assertThat(e.status()).isEqualTo(Gradebook.Status.SUBMITTED); // the new version still needs grading
    }

    @Test
    void gradebookShowsStatisticsPerItem() {
        Assignment a = assignment("Hand-in 1", FRIDAY_23_59, true, false);
        Submission sa = submissionService.submit(a.getId(), anna.getId(), "report A");
        Submission sb = submissionService.submit(a.getId(), bob.getId(), "report B");
        gradingService.grade(sa.getId(), teacher.getId(), 6, null);
        gradingService.grade(sb.getId(), teacher.getId(), 10, null);
        // Carl does not submit

        Gradebook book = gradingService.gradebook(course.getId());

        Gradebook.Item item = book.items().get(0);
        assertThat(book.students()).hasSize(3);  // the teacher is not listed
        assertThat(item.submittedCount()).isEqualTo(2);
        assertThat(item.gradedCount()).isEqualTo(2);
        assertThat(item.averagePoints()).isEqualTo(8.0);
        assertThat(row(book, bob).totalPoints()).isEqualTo(10.0);
    }

    @Test
    void deletingCourseRemovesSubmissionsGradesAndGroups() {
        Assignment a = assignment("Group project", FRIDAY_23_59, false, true);
        StudentGroup group = groupService.create(course.getId(), "Group 1");
        groupService.addMember(group.getId(), "anna@hvl.no");
        Submission s = submissionService.submit(a.getId(), anna.getId(), "repo");
        gradingService.grade(s.getId(), teacher.getId(), 5, null);

        courseService.delete(course.getId());
        entityManager.flush(); // run the deletes now, so foreign-key problems would show up

        assertThatThrownBy(() -> submissionService.get(s.getId())).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> groupService.get(group.getId())).isInstanceOf(NotFoundException.class);
    }

    // --- helpers ---

    private Assignment assignment(String title, Instant deadline, boolean lateAllowed, boolean groupWork) {
        Assignment a = new Assignment();
        a.setTitle(title);
        a.setDeadline(deadline);
        a.setLateSubmissionsAllowed(lateAllowed);
        a.setGroupWork(groupWork);
        a.setMaxPoints(10);
        return contentService.add(week1.getId(), a);
    }

    private Gradebook.StudentRow row(Gradebook book, User student) {
        return book.students().stream().filter(r -> r.userId().equals(student.getId())).findFirst().orElseThrow();
    }

    private Gradebook.Entry entry(Gradebook book, User student, GradedContent item) {
        return row(book, student).entries().stream()
                .filter(e -> e.contentId().equals(item.getId())).findFirst().orElseThrow();
    }
}
