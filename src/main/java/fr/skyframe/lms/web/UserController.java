package fr.skyframe.lms.web;

import fr.skyframe.lms.service.CourseService;
import fr.skyframe.lms.service.UserService;
import fr.skyframe.lms.web.dto.CreateUserRequest;
import fr.skyframe.lms.web.dto.EnrollmentResponse;
import fr.skyframe.lms.web.dto.UserResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final CourseService courseService;

    public UserController(UserService userService, CourseService courseService) {
        this.userService = userService;
        this.courseService = courseService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse create(@Valid @RequestBody CreateUserRequest request) {
        return UserResponse.from(userService.create(request.email(), request.name()));
    }

    @GetMapping
    public List<UserResponse> list() {
        return userService.list().stream().map(UserResponse::from).toList();
    }

    @GetMapping("/{id}")
    public UserResponse get(@PathVariable Long id) {
        return UserResponse.from(userService.get(id));
    }

    /** All courses the user takes part in, with their role in each. */
    @GetMapping("/{id}/enrollments")
    public List<EnrollmentResponse> enrollments(@PathVariable Long id) {
        return courseService.coursesOfUser(id).stream().map(EnrollmentResponse::from).toList();
    }
}