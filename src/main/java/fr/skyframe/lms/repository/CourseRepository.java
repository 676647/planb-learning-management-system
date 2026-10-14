package fr.skyframe.lms.repository;

import fr.skyframe.lms.domain.Course;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseRepository extends JpaRepository<Course, Long> {
}
