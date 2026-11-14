package fr.skyframe.lms.repository;

import fr.skyframe.lms.domain.CourseModule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CourseModuleRepository extends JpaRepository<CourseModule, Long> {

    List<CourseModule> findByCourseIdOrderByPositionAsc(Long courseId);

    /** The module with the highest position in a course, used to append new modules at the end. */
    Optional<CourseModule> findTopByCourseIdOrderByPositionDesc(Long courseId);

    /** Modules that require the given module. */
    List<CourseModule> findByRequiresId(Long requiredModuleId);
}
