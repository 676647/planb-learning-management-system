package fr.skyframe.lms.service;

import fr.skyframe.lms.domain.User;
import fr.skyframe.lms.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class UserService {

    private final UserRepository users;

    public UserService(UserRepository users) {
        this.users = users;
    }

    public User create(String email, String name) {
        String normalized = email.trim().toLowerCase();
        if (users.existsByEmail(normalized)) {
            throw new ConflictException("A user with e-mail " + normalized + " already exists");
        }
        User user = new User();
        user.setEmail(normalized);
        user.setName(name);
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
