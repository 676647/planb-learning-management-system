package fr.skyframe.lms.repository;

import fr.skyframe.lms.domain.Grade;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GradeRepository extends JpaRepository<Grade, Long> {

    Optional<Grade> findBySubmissionId(Long submissionId);

    /** All grades in a course (for the gradebook). */
    List<Grade> findBySubmissionContentModuleCourseId(Long courseId);

    void deleteBySubmissionContentId(Long contentId);
}
