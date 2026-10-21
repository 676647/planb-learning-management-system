package fr.skyframe.lms.security;

import fr.skyframe.lms.domain.User;
import fr.skyframe.lms.service.UserService;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/**
 * Gives access to the logged-in user of the current request.
 * Spring Security has already checked the token; its subject is the user's e-mail.
 */
@Component
public class CurrentUser {

    private final UserService userService;

    public CurrentUser(UserService userService) {
        this.userService = userService;
    }

    public User get() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof Jwt jwt)) {
            throw new AuthenticationCredentialsNotFoundException("Not logged in");
        }
        return userService.getByEmail(jwt.getSubject());
    }

    public Long id() {
        return get().getId();
    }
}
