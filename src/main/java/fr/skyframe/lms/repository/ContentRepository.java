package fr.skyframe.lms.repository;

import fr.skyframe.lms.domain.Content;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** Works for all content types (Page, Quiz, ...) because they share one table. */
public interface ContentRepository extends JpaRepository<Content, Long> {

    List<Content> findByModuleIdOrderByPositionAsc(Long moduleId);

    Optional<Content> findTopByModuleIdOrderByPositionDesc(Long moduleId);

    void deleteByModuleId(Long moduleId);
}
