package fr.skyframe.lms.repository;

import fr.skyframe.lms.domain.QuizAnswer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuizAnswerRepository extends JpaRepository<QuizAnswer, Long> {

    List<QuizAnswer> findBySubmissionIdOrderByQuestionPositionAsc(Long submissionId);

    void deleteBySubmissionContentId(Long contentId);
}
