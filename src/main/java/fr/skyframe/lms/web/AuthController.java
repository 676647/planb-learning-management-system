package fr.skyframe.lms.web;

import fr.skyframe.lms.security.TokenService;
import fr.skyframe.lms.service.AuthService;
import fr.skyframe.lms.service.UserService;
import fr.skyframe.lms.web.dto.LoginRequest;
import fr.skyframe.lms.web.dto.RegisterRequest;
import fr.skyframe.lms.web.dto.TokenResponse;
import fr.skyframe.lms.web.dto.UserResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/** Public endpoints (no token needed): sign up and log in. */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final AuthService authService;

    public AuthController(UserService userService, AuthService authService) {
        this.userService = userService;
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@Valid @RequestBody RegisterRequest request) {
        return UserResponse.from(userService.register(request.email(), request.name(), request.password()));
    }

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        TokenService.IssuedToken token = authService.login(request.email(), request.password());
        return TokenResponse.bearer(token.value(), token.expiresAt());
    }
}
