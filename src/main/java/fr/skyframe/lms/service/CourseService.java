package fr.skyframe.lms.service;

import fr.skyframe.lms.domain.Course;
import fr.skyframe.lms.domain.Enrollment;
import fr.skyframe.lms.domain.Role;
import fr.skyframe.lms.domain.User;
import fr.skyframe.lms.events.UserEnrolled;
import fr.skyframe.lms.repository.CourseRepository;
import org.springframework.context.ApplicationEventPublisher;
import fr.skyframe.lms.repository.EnrollmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

/**
 * Business logic for US1 (course administration).
 * Controllers stay thin and call this class; this class talks to the repositories.
 */
@Service
@Transactional
public class CourseService {

    private final CourseRepository courses;
    private final EnrollmentRepository enrollments;
    private final UserService userService;
    private final ModuleService moduleService;
    private final GroupService groupService;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public CourseService(CourseRepository courses, EnrollmentRepository enrollments,
                         UserService userService, ModuleService moduleService, GroupService groupService,
                         ApplicationEventPublisher events, Clock clock) {
        this.courses = courses;
        this.enrollments = enrollments;
        this.userService = userService;
        this.moduleService = moduleService;
        this.groupService = groupService;
        this.events = events;
        this.clock = clock;
    }

    /** Creates a course and enrolls the creator as its teacher. */
    public Course create(Long teacherId, String title, String description, String semester) {
        User teacher = userService.get(teacherId);

        Course course = new Course();
        course.setTitle(title);
        course.setDescription(description);
        course.setSemester(semester);
        course = courses.save(course);

        enroll(teacher, course, Role.TEACHER);
        return course;
    }

    @Transactional(readOnly = true)
    public Course get(Long id) {
        return courses.findById(id)
                .orElseThrow(() -> new NotFoundException("Course " + id + " not found"));
    }

    @Transactional(readOnly = true)
    public List<Course> list() {
        return courses.findAll();
    }

    public Course update(Long id, String title, String description, String semester) {
        Course course = get(id);
        course.setTitle(title);
        course.setDescription(description);
        course.setSemester(semester);
        return course; // changes are saved automatically when the transaction commits
    }

    public void delete(Long id) {
        Course course = get(id);
        moduleService.deleteAllOfCourse(course.getId()); // also removes submissions and grades
        groupService.deleteAllOfCourse(course.getId());
        enrollments.deleteByCourseId(course.getId());
        courses.delete(course);
    }

    /** Adds an existing user (looked up by e-mail) to a course with the given role. */
    public Enrollment enroll(Long courseId, String email, Role role) {
        Course course = get(courseId);
        User user = userService.getByEmail(email);
        Enrollment enrollment = enroll(user, course, role);
        events.publishEvent(new UserEnrolled(UUID.randomUUID(), clock.instant(),
                course.getId(), course.getTitle(), user.getId(), role));
        return enrollment;
    }

    @Transactional(readOnly = true)
    public List<Enrollment> participants(Long courseId) {
        get(courseId); // 404 if the course does not exist
        return enrollments.findByCourseId(courseId);
    }

    @Transactional(readOnly = true)
    public List<Enrollment> coursesOfUser(Long userId) {
        userService.get(userId);
        return enrollments.findByUserId(userId);
    }

    /**
     * Removes a user from a course, and from their group in it. The last teacher can't be removed,
     * because nobody could manage the course after that; add another teacher first or delete the course.
     */
    public void unenroll(Long courseId, Long userId) {
        Enrollment enrollment = enrollments.findByUserIdAndCourseId(userId, courseId)
                .orElseThrow(() -> new NotFoundException("User " + userId + " is not enrolled in course " + courseId));
        if (enrollment.getRole() == Role.TEACHER && enrollments.countByCourseIdAndRole(courseId, Role.TEACHER) <= 1) {
            throw new ConflictException("The last teacher can't leave course " + courseId
                    + "; add another teacher first, or delete the course");
        }
        groupService.leaveGroupIn(courseId, userId);
        enrollments.delete(enrollment);
    }

    private Enrollment enroll(User user, Course course, Role role) {
        if (enrollments.existsByUserIdAndCourseId(user.getId(), course.getId())) {
            throw new ConflictException(user.getEmail() + " is already enrolled in course " + course.getId());
        }
        Enrollment enrollment = new Enrollment();
        enrollment.setUser(user);
        enrollment.setCourse(course);
        enrollment.setRole(role);
        return enrollments.save(enrollment);
    }
}
