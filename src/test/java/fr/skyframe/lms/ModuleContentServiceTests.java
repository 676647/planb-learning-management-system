package fr.skyframe.lms;

import fr.skyframe.lms.domain.*;
import fr.skyframe.lms.service.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests ordering, dependencies and deletion of modules and contents through the services.
 * @Transactional rolls the database back after each test.
 */
@SpringBootTest
@Transactional
class ModuleContentServiceTests {

    @Autowired UserService userService;
    @Autowired CourseService courseService;
    @Autowired ModuleService moduleService;
    @Autowired ContentService contentService;
    @Autowired EntityManager entityManager;

    Course course;

    @BeforeEach
    void createCourse() {
        User teacher = userService.register("teacher@hvl.no", "Tina Teacher", "secret-password");
        course = courseService.create(teacher.getId(), "DAT250", null, "Autumn 2026");
    }

    @Test
    void modulesAreAppendedInOrder() {
        moduleService.create(course.getId(), "Week 1");
        moduleService.create(course.getId(), "Week 2");
        moduleService.create(course.getId(), "Week 3");

        assertThat(moduleService.listForCourse(course.getId()))
                .extracting(CourseModule::getTitle)
                .containsExactly("Week 1", "Week 2", "Week 3");
    }

    @Test
    void modulesCanBeReordered() {
        CourseModule w1 = moduleService.create(course.getId(), "Week 1");
        CourseModule w2 = moduleService.create(course.getId(), "Week 2");
        CourseModule w3 = moduleService.create(course.getId(), "Week 3");

        moduleService.reorder(course.getId(), List.of(w3.getId(), w1.getId(), w2.getId()));

        assertThat(moduleService.listForCourse(course.getId()))
                .extracting(CourseModule::getTitle)
                .containsExactly("Week 3", "Week 1", "Week 2");
    }

    @Test
    void reorderMustListEveryModule() {
        CourseModule w1 = moduleService.create(course.getId(), "Week 1");
        moduleService.create(course.getId(), "Week 2");

        assertThatThrownBy(() -> moduleService.reorder(course.getId(), List.of(w1.getId())))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void cyclicDependenciesAreRejected() {
        CourseModule a = moduleService.create(course.getId(), "A");
        CourseModule b = moduleService.create(course.getId(), "B");
        CourseModule c = moduleService.create(course.getId(), "C");
        moduleService.addRequirement(b.getId(), a.getId()); // B requires A
        moduleService.addRequirement(c.getId(), b.getId()); // C requires B

        // A requires C would close the loop A -> C -> B -> A
        assertThatThrownBy(() -> moduleService.addRequirement(a.getId(), c.getId()))
                .isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> moduleService.addRequirement(a.getId(), a.getId()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void moduleCannotRequireModuleOfAnotherCourse() {
        User other = userService.register("other@hvl.no", "Other Teacher", "secret-password");
        Course otherCourse = courseService.create(other.getId(), "DAT251", null, null);
        CourseModule here = moduleService.create(course.getId(), "Here");
        CourseModule there = moduleService.create(otherCourse.getId(), "There");

        assertThatThrownBy(() -> moduleService.addRequirement(here.getId(), there.getId()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void contentsOfDifferentTypesShareOneOrder() {
        CourseModule week1 = moduleService.create(course.getId(), "Week 1");
        Page page = new Page();
        page.setTitle("Introduction");
        Quiz quiz = new Quiz();
        quiz.setTitle("Check your understanding");
        Assignment assignment = new Assignment();
        assignment.setTitle("Hand-in 1");

        contentService.add(week1.getId(), page);
        contentService.add(week1.getId(), quiz);
        contentService.add(week1.getId(), assignment);

        assertThat(contentService.listForModule(week1.getId()))
                .extracting(Content::getType)
                .containsExactly(ContentType.PAGE, ContentType.QUIZ, ContentType.ASSIGNMENT);
    }

    @Test
    void studentViewHidesItemsOutsideTheirWindow() {
        CourseModule week1 = moduleService.create(course.getId(), "Week 1");
        Page published = new Page();
        published.setTitle("Published");
        Page upcoming = new Page();
        upcoming.setTitle("Next week");
        upcoming.setVisibleFrom(Instant.now().plus(Duration.ofDays(7)));
        contentService.add(week1.getId(), published);
        contentService.add(week1.getId(), upcoming);

        assertThat(contentService.listForModule(week1.getId())).hasSize(2);
        assertThat(contentService.listVisibleForModule(week1.getId()))
                .extracting(Content::getTitle)
                .containsExactly("Published");
    }

    @Test
    void invalidVisibilityWindowIsRejected() {
        CourseModule week1 = moduleService.create(course.getId(), "Week 1");
        Page page = new Page();
        page.setTitle("Broken window");
        page.setVisibleFrom(Instant.parse("2026-11-01T00:00:00Z"));
        page.setVisibleUntil(Instant.parse("2026-10-01T00:00:00Z"));

        assertThatThrownBy(() -> contentService.add(week1.getId(), page))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deletingModuleRemovesItsContentsAndDependenciesOnIt() {
        CourseModule a = moduleService.create(course.getId(), "A");
        CourseModule b = moduleService.create(course.getId(), "B");
        moduleService.addRequirement(b.getId(), a.getId());
        Page page = new Page();
        page.setTitle("In A");
        Page saved = contentService.add(a.getId(), page);

        moduleService.delete(a.getId());
        entityManager.flush(); // send the deletes to the database now, so foreign-key problems would show up

        assertThatThrownBy(() -> contentService.get(saved.getId())).isInstanceOf(NotFoundException.class);
        assertThat(moduleService.get(b.getId()).getRequires()).isEmpty();
    }

    @Test
    void deletingCourseRemovesItsModulesAndContents() {
        CourseModule a = moduleService.create(course.getId(), "A");
        CourseModule b = moduleService.create(course.getId(), "B");
        moduleService.addRequirement(b.getId(), a.getId());
        Page page = new Page();
        page.setTitle("In A");
        contentService.add(a.getId(), page);

        courseService.delete(course.getId());
        entityManager.flush();

        assertThatThrownBy(() -> moduleService.get(a.getId())).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> moduleService.get(b.getId())).isInstanceOf(NotFoundException.class);
    }
}
