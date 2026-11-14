package fr.skyframe.lms.repository;

import fr.skyframe.lms.domain.GradedContent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Only the gradable content items (quizzes, coding exercises, assignments). */
public interface GradedContentRepository extends JpaRepository<GradedContent, Long> {

    /** All gradable items of a course, in course order (module position, then item position). */
    List<GradedContent> findByModuleCourseIdOrderByModulePositionAscPositionAsc(Long courseId);
}
