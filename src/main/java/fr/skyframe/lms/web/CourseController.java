package fr.skyframe.lms.web;

import fr.skyframe.lms.security.CurrentUser;
import fr.skyframe.lms.service.CourseService;
import fr.skyframe.lms.web.dto.CourseRequest;
import fr.skyframe.lms.web.dto.CourseResponse;
import fr.skyframe.lms.web.dto.EnrollRequest;
import fr.skyframe.lms.web.dto.EnrollmentResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API for US1: teachers create and administer courses and invite participants.
 * Every endpoint needs a logged-in user; @PreAuthorize adds the per-course rules.
 */
@RestController
@RequestMapping("/api/courses")
public class CourseController {

    private final CourseService courseService;
    private final CurrentUser currentUser;

    public CourseController(CourseService courseService, CurrentUser currentUser) {
        this.courseService = courseService;
        this.currentUser = currentUser;
    }

    /** Teachers (and admins) can create a course, and become its teacher. */
    @PostMapping
    @PreAuthorize("@access.canCreateCourses()")
    @ResponseStatus(HttpStatus.CREATED)
    public CourseResponse create(@Valid @RequestBody CourseRequest request) {
        return CourseResponse.from(courseService.create(
                currentUser.id(), request.title(), request.description(), request.semester()));
    }

    /** The course catalogue is visible to every logged-in user. */
    @GetMapping
    public List<CourseResponse> list() {
        return courseService.list().stream().map(CourseResponse::from).toList();
    }

    @GetMapping("/{id}")
    public CourseResponse get(@PathVariable Long id) {
        return CourseResponse.from(courseService.get(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@access.isTeacherOfCourse(#id)")
    public CourseResponse update(@PathVariable Long id, @Valid @RequestBody CourseRequest request) {
        return CourseResponse.from(courseService.update(
                id, request.title(), request.description(), request.semester()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@access.isTeacherOfCourse(#id)")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        courseService.delete(id);
    }

    /** Teachers invite users into their course. */
    @PostMapping("/{id}/enrollments")
    @PreAuthorize("@access.isTeacherOfCourse(#id)")
    @ResponseStatus(HttpStatus.CREATED)
    public EnrollmentResponse enroll(@PathVariable Long id, @Valid @RequestBody EnrollRequest request) {
        return EnrollmentResponse.from(courseService.enroll(id, request.email(), request.role()));
    }

    /** Only participants of a course can see who else is in it. */
    @GetMapping("/{id}/enrollments")
    @PreAuthorize("@access.isMemberOfCourse(#id)")
    public List<EnrollmentResponse> participants(@PathVariable Long id) {
        return courseService.participants(id).stream().map(EnrollmentResponse::from).toList();
    }

    /** Teachers can remove anyone; other users can only remove themselves (leave the course). */
    @DeleteMapping("/{id}/enrollments/{userId}")
    @PreAuthorize("@access.isTeacherOfCourse(#id) or @access.isMe(#userId)")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unenroll(@PathVariable Long id, @PathVariable Long userId) {
        courseService.unenroll(id, userId);
    }
}
