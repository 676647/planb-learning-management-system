package fr.skyframe.lms.repository;

import fr.skyframe.lms.domain.StudentGroup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StudentGroupRepository extends JpaRepository<StudentGroup, Long> {

    List<StudentGroup> findByCourseId(Long courseId);

    /** The group a user belongs to in a course, if any. */
    Optional<StudentGroup> findByCourseIdAndMembersId(Long courseId, Long userId);
}
