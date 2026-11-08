package fr.skyframe.lms.service;

import fr.skyframe.lms.repository.GradeRepository;
import fr.skyframe.lms.repository.QuestionRepository;
import fr.skyframe.lms.repository.QuizAnswerRepository;
import fr.skyframe.lms.repository.SubmissionRepository;
import org.springframework.stereotype.Component;

/**
 * Deletes everything that belongs to a content item, before the item itself is deleted.
 * The order matters because of foreign keys: quiz answers and grades point to submissions,
 * submissions and questions point to the item.
 * Used by ContentService (one item) and ModuleService (all items of a module or course).
 */
@Component
public class ContentDataCleaner {

    private final StoredFileService files;
    private final QuizAnswerRepository quizAnswers;
    private final GradeRepository grades;
    private final SubmissionRepository submissions;
    private final QuestionRepository questions;

    public ContentDataCleaner(StoredFileService files, QuizAnswerRepository quizAnswers, GradeRepository grades,
                              SubmissionRepository submissions, QuestionRepository questions) {
        this.files = files;
        this.quizAnswers = quizAnswers;
        this.grades = grades;
        this.submissions = submissions;
        this.questions = questions;
    }

    public void deleteDataOf(Long contentId) {
        files.deleteFilesOfContent(contentId);   // uploaded files in MongoDB, after commit
        quizAnswers.deleteBySubmissionContentId(contentId);
        grades.deleteBySubmissionContentId(contentId);
        submissions.deleteByContentId(contentId);
        questions.deleteByQuizId(contentId);
    }
}
