package fr.skyframe.lms.web;

import fr.skyframe.lms.security.CurrentUser;
import fr.skyframe.lms.service.CourseService;
import fr.skyframe.lms.service.UserService;
import fr.skyframe.lms.web.dto.EnrollmentResponse;
import fr.skyframe.lms.web.dto.UserResponse;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Users register and log in through AuthController; this controller is for reading user data. */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final CourseService courseService;
    private final CurrentUser currentUser;

    public UserController(UserService userService, CourseService courseService, CurrentUser currentUser) {
        this.userService = userService;
        this.courseService = courseService;
        this.currentUser = currentUser;
    }

    /** The logged-in user. */
    @GetMapping("/me")
    public UserResponse me() {
        return UserResponse.from(currentUser.get());
    }

    /** Courses the logged-in user takes part in, with their role in each. */
    @GetMapping("/me/enrollments")
    public List<EnrollmentResponse> myEnrollments() {
        return courseService.coursesOfUser(currentUser.id()).stream().map(EnrollmentResponse::from).toList();
    }

    /** Name and e-mail of any user (e.g. to show who is in a course). */
    @GetMapping("/{id}")
    public UserResponse get(@PathVariable Long id) {
        return UserResponse.from(userService.get(id));
    }
}
