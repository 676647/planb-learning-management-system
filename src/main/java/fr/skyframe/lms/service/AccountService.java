package fr.skyframe.lms.service;

import fr.skyframe.lms.domain.AccountRole;
import fr.skyframe.lms.domain.User;
import fr.skyframe.lms.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Account-wide roles: who is a teacher (may create courses) and who is an admin.
 *
 * Every new account is a STUDENT. Only an admin decides who becomes a TEACHER or ADMIN;
 * users can't ask for it themselves.
 */
@Service
@Transactional
public class AccountService {

    private final UserRepository users;
    private final UserService userService;

    public AccountService(UserRepository users, UserService userService) {
        this.users = users;
        this.userService = userService;
    }

    @Transactional(readOnly = true)
    public List<User> listUsers() {
        return users.findAllByOrderByNameAscIdAsc();
    }

    /**
     * Sets a user's account role. Taking away a teacher's role doesn't remove them
     * from courses they already teach; it only stops them from creating new ones.
     */
    public User setRole(Long userId, AccountRole role, Long adminId) {
        User user = userService.get(userId);
        if (userId.equals(adminId) && role != AccountRole.ADMIN) {
            // Otherwise the last admin could lock everyone out of the admin page
            throw new ConflictException("You can't remove your own admin role; ask another admin");
        }
        user.setAccountRole(role);
        return user;
    }
}
