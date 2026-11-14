package fr.skyframe.lms.repository;

import fr.skyframe.lms.domain.LiveSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LiveSessionRepository extends JpaRepository<LiveSession, Long> {

    List<LiveSession> findByQuizIdOrderByEndedAtDescIdDesc(Long quizId);

    /** Loads the sessions and deletes them one by one, so their players are deleted too (cascade). */
    void deleteByQuizId(Long quizId);
}
