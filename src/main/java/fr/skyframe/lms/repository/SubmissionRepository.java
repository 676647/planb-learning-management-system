package fr.skyframe.lms.repository;

import fr.skyframe.lms.domain.Submission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SubmissionRepository extends JpaRepository<Submission, Long> {

    List<Submission> findByContentIdOrderBySubmittedAtDesc(Long contentId);

    /** All submissions for all graded items of a course (for the gradebook). */
    List<Submission> findByContentModuleCourseId(Long courseId);

    long countByContentIdAndSubmittedById(Long contentId, Long userId);

    void deleteByContentId(Long contentId);
}
