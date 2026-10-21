package fr.skyframe.lms.service;

import fr.skyframe.lms.domain.User;
import fr.skyframe.lms.repository.UserRepository;
import fr.skyframe.lms.security.TokenService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    public AuthService(UserRepository users, PasswordEncoder passwordEncoder, TokenService tokenService) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
    }

    /** Checks e-mail and password and returns a signed token if they match. */
    public TokenService.IssuedToken login(String email, String password) {
        User user = users.findByEmail(email.trim().toLowerCase())
                .orElseThrow(InvalidCredentialsException::new);
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            // Same error for unknown e-mail and wrong password, so attackers
            // cannot find out which e-mail addresses are registered.
            throw new InvalidCredentialsException();
        }
        return tokenService.issue(user);
    }
}
