package fr.skyframe.lms.repository;

import fr.skyframe.lms.domain.Question;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** Works for all question types, because they share one table. */
public interface QuestionRepository extends JpaRepository<Question, Long> {

    List<Question> findByQuizIdOrderByPositionAsc(Long quizId);

    Optional<Question> findTopByQuizIdOrderByPositionDesc(Long quizId);

    void deleteByQuizId(Long quizId);
}
