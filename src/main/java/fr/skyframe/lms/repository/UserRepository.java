package fr.skyframe.lms.repository;

import fr.skyframe.lms.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data generates the implementation at runtime.
 * Method names like findByEmail are turned into SQL queries automatically.
 */
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    List<User> findAllByOrderByNameAscIdAsc();
}
