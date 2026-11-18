package fr.skyframe.lms.repository;

import fr.skyframe.lms.domain.Enrollment;
import fr.skyframe.lms.domain.Role;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    List<Enrollment> findByCourseId(Long courseId);

    List<Enrollment> findByUserId(Long userId);

    Optional<Enrollment> findByUserIdAndCourseId(Long userId, Long courseId);

    boolean existsByUserIdAndCourseId(Long userId, Long courseId);

    boolean existsByUserIdAndCourseIdAndRole(Long userId, Long courseId, Role role);

    long countByCourseIdAndRole(Long courseId, Role role);

    /** True if the two users are both enrolled (in any role) in at least one common course. */
    @Query("select case when count(a) > 0 then true else false end from Enrollment a, Enrollment b "
            + "where a.course = b.course and a.user.id = :userId and b.user.id = :otherUserId")
    boolean shareACourse(@Param("userId") Long userId, @Param("otherUserId") Long otherUserId);

    /**
     * Loads a user's enrollment and locks its row until the transaction ends (SELECT ... FOR UPDATE).
     * Another transaction asking for the same lock waits, so work done under it runs one at a time per user and course.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Enrollment e where e.user.id = :userId and e.course.id = :courseId")
    Optional<Enrollment> lockByUserIdAndCourseId(@Param("userId") Long userId, @Param("courseId") Long courseId);

    void deleteByCourseId(Long courseId);
}
