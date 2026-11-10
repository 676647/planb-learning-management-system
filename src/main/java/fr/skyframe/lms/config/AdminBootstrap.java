package fr.skyframe.lms.config;

import fr.skyframe.lms.domain.AccountRole;
import fr.skyframe.lms.domain.User;
import fr.skyframe.lms.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates the first admin when the application starts, so someone can make users teachers.
 *
 * Configured with lms.admin.email and lms.admin.password (LMS_ADMIN_EMAIL / LMS_ADMIN_PASSWORD).
 * - No account with that e-mail yet: it is created as an admin with that password.
 * - The account exists: it is made an admin (its password is left alone).
 * - No e-mail configured: nothing happens.
 */
@Component
public class AdminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final String email;
    private final String password;
    private final String name;

    public AdminBootstrap(UserRepository users, PasswordEncoder passwordEncoder,
                          @Value("${lms.admin.email:}") String email,
                          @Value("${lms.admin.password:}") String password,
                          @Value("${lms.admin.name:Administrator}") String name) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.email = email.trim().toLowerCase();
        this.password = password;
        this.name = name;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (email.isEmpty()) {
            return;
        }
        users.findByEmail(email).ifPresentOrElse(user -> {
            if (user.getAccountRole() != AccountRole.ADMIN) {
                user.setAccountRole(AccountRole.ADMIN);
                log.info("Made {} an admin", email);
            }
        }, () -> {
            if (password.length() < 8) {
                log.warn("lms.admin.password must be at least 8 characters; admin {} was not created", email);
                return;
            }
            User admin = new User();
            admin.setEmail(email);
            admin.setName(name);
            admin.setPasswordHash(passwordEncoder.encode(password));
            admin.setAccountRole(AccountRole.ADMIN);
            users.save(admin);
            log.info("Created admin account {}", email);
        });
    }
}
