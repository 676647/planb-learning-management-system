package fr.skyframe.lms.service;

import fr.skyframe.lms.domain.User;
import fr.skyframe.lms.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class UserService {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository users, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    /** Registers a new user. The password is stored as a BCrypt hash. */
    public User register(String email, String name, String password) {
        String normalized = email.trim().toLowerCase();
        if (users.existsByEmail(normalized)) {
            throw new ConflictException("A user with e-mail " + normalized + " already exists");
        }
        User user = new User();
        user.setEmail(normalized);
        user.setName(name);
        user.setPasswordHash(passwordEncoder.encode(password));
        return users.save(user);
    }

    @Transactional(readOnly = true)
    public User get(Long id) {
        return users.findById(id)
                .orElseThrow(() -> new NotFoundException("User " + id + " not found"));
    }

    @Transactional(readOnly = true)
    public User getByEmail(String email) {
        return users.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new NotFoundException("User with e-mail " + email + " not found"));
    }

    @Transactional(readOnly = true)
    public List<User> list() {
        return users.findAll();
    }
}
