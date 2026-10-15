package fr.skyframe.lms.web;

import fr.skyframe.lms.service.CourseService;
import fr.skyframe.lms.web.dto.CourseRequest;
import fr.skyframe.lms.web.dto.CourseResponse;
import fr.skyframe.lms.web.dto.EnrollRequest;
import fr.skyframe.lms.web.dto.EnrollmentResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** REST API for US1: teachers create and administer courses and invite participants. */
@RestController
@RequestMapping("/api/courses")
public class CourseController {

    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CourseResponse create(@Valid @RequestBody CourseRequest request) {
        if (request.teacherId() == null) {
            throw new IllegalArgumentException("teacherId is required when creating a course");
        }
        return CourseResponse.from(courseService.create(
                request.teacherId(), request.title(), request.description(), request.semester()));
    }

    @GetMapping
    public List<CourseResponse> list() {
        return courseService.list().stream().map(CourseResponse::from).toList();
    }

    @GetMapping("/{id}")
    public CourseResponse get(@PathVariable Long id) {
        return CourseResponse.from(courseService.get(id));
    }

    @PutMapping("/{id}")
    public CourseResponse update(@PathVariable Long id, @Valid @RequestBody CourseRequest request) {
        return CourseResponse.from(courseService.update(
                id, request.title(), request.description(), request.semester()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        courseService.delete(id);
    }

    @PostMapping("/{id}/enrollments")
    @ResponseStatus(HttpStatus.CREATED)
    public EnrollmentResponse enroll(@PathVariable Long id, @Valid @RequestBody EnrollRequest request) {
        return EnrollmentResponse.from(courseService.enroll(id, request.email(), request.role()));
    }

    @GetMapping("/{id}/enrollments")
    public List<EnrollmentResponse> participants(@PathVariable Long id) {
        return courseService.participants(id).stream().map(EnrollmentResponse::from).toList();
    }

    @DeleteMapping("/{id}/enrollments/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unenroll(@PathVariable Long id, @PathVariable Long userId) {
        courseService.unenroll(id, userId);
    }
}