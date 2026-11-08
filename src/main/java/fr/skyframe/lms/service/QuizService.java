package fr.skyframe.lms.service;

import fr.skyframe.lms.domain.*;
import fr.skyframe.lms.repository.QuestionRepository;
import fr.skyframe.lms.repository.QuizAnswerRepository;
import fr.skyframe.lms.repository.SubmissionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Quizzes: teachers add questions; students answer them in attempts, which are graded
 * automatically. Each attempt is a Submission, so deadlines, attempt limits, the gradebook and
 * the RabbitMQ events work for quizzes like for other items. The best attempt counts.
 */
@Service
@Transactional
public class QuizService {

    private final QuestionRepository questions;
    private final QuizAnswerRepository quizAnswers;
    private final SubmissionRepository submissions;
    private final ContentService contentService;
    private final SubmissionService submissionService;
    private final GradingService gradingService;
    private final Random random = new Random();

    public QuizService(QuestionRepository questions, QuizAnswerRepository quizAnswers,
                       SubmissionRepository submissions, ContentService contentService,
                       SubmissionService submissionService, GradingService gradingService) {
        this.questions = questions;
        this.quizAnswers = quizAnswers;
        this.submissions = submissions;
        this.contentService = contentService;
        this.submissionService = submissionService;
        this.gradingService = gradingService;
    }

    // --- questions (teachers) ---

    @Transactional(readOnly = true)
    public Quiz getQuiz(Long quizId) {
        if (contentService.get(quizId) instanceof Quiz quiz) {
            return quiz;
        }
        throw new IllegalArgumentException("Content " + quizId + " is not a quiz");
    }

    @Transactional(readOnly = true)
    public List<Question> questionsOf(Long quizId) {
        getQuiz(quizId);
        return questions.findByQuizIdOrderByPositionAsc(quizId);
    }

    @Transactional(readOnly = true)
    public Question getQuestion(Long questionId) {
        return questions.findById(questionId)
                .orElseThrow(() -> new NotFoundException("Question " + questionId + " not found"));
    }

    /** Adds a question at the end of the quiz. The quiz's max points become the sum of its questions' points. */
    public <T extends Question> T addQuestion(Long quizId, T question) {
        Quiz quiz = getQuiz(quizId);
        requireNoAttempts(quiz);
        if (question.getPrompt() == null || question.getPrompt().isBlank()) {
            throw new IllegalArgumentException("The question text is required");
        }
        if (question.getPoints() < 0) {
            throw new IllegalArgumentException("Points can't be negative");
        }
        question.validate();
        question.setQuiz(quiz);
        question.setPosition(questions.findTopByQuizIdOrderByPositionDesc(quizId)
                .map(last -> last.getPosition() + 1).orElse(0));
        T saved = questions.save(question);
        updateMaxPoints(quiz);
        return saved;
    }

    public void deleteQuestion(Long questionId) {
        Question question = getQuestion(questionId);
        Quiz quiz = question.getQuiz();
        requireNoAttempts(quiz);
        questions.delete(question);
        updateMaxPoints(quiz);
    }

    /**
     * Once students have answered, changing the questions would make earlier results meaningless
     * (and unfair between students), so the questions are frozen.
     */
    private void requireNoAttempts(Quiz quiz) {
        if (submissions.existsByContentId(quiz.getId())) {
            throw new ConflictException("Students have already answered '" + quiz.getTitle()
                    + "', so its questions can no longer be changed");
        }
    }

    private void updateMaxPoints(Quiz quiz) {
        quiz.setMaxPoints(questions.findByQuizIdOrderByPositionAsc(quiz.getId()).stream()
                .mapToInt(Question::getPoints).sum());
    }

    // --- answering (students) ---

    /** The questions as students see them: shuffled where needed, never with the solutions. */
    @Transactional(readOnly = true)
    public List<StudentQuestion> studentView(Long quizId) {
        return questionsOf(quizId).stream().map(this::toStudentView).toList();
    }

    /**
     * A student answers the whole quiz. The answers are graded straight away; the result is
     * returned and stored. answers maps question id → answer in that question type's format.
     */
    public QuizAttemptResult attempt(Long quizId, Long studentId, Map<Long, List<String>> answers) {
        Quiz quiz = getQuiz(quizId);
        List<Question> quizQuestions = questions.findByQuizIdOrderByPositionAsc(quizId);
        if (quizQuestions.isEmpty()) {
            throw new IllegalArgumentException("This quiz has no questions yet");
        }
        // Checks student, visibility, deadline and attempt limit, saves the attempt, publishes the event
        Submission submission = submissionService.createSubmission(quiz, studentId, "Quiz attempt", null);

        double total = 0;
        int fullyCorrect = 0;
        for (Question question : quizQuestions) {
            List<String> given = answers == null ? List.of() : answers.getOrDefault(question.getId(), List.of());
            double fraction = Math.max(0, Math.min(1, question.score(given)));
            QuizAnswer answer = new QuizAnswer();
            answer.setSubmission(submission);
            answer.setQuestion(question);
            answer.setAnswer(given);
            answer.setEarnedPoints(round(question.getPoints() * fraction));
            answer.setCorrect(fraction >= 0.9999);
            quizAnswers.save(answer);
            total += answer.getEarnedPoints();
            if (answer.isCorrect()) {
                fullyCorrect++;
            }
        }
        String summary = fullyCorrect + " of " + quizQuestions.size() + " questions fully correct";
        submission.setAnswer("Quiz attempt: " + summary);
        gradingService.autoGrade(submission, round(total), "Graded automatically: " + summary + ".");
        return resultOf(submission, false);
    }

    /** All of a student's attempts at a quiz, newest first. */
    @Transactional(readOnly = true)
    public List<QuizAttemptResult> attemptsOf(Long quizId, Long studentId) {
        getQuiz(quizId);
        return submissions.findByContentIdAndSubmittedByIdOrderBySubmittedAtDescIdDesc(quizId, studentId).stream()
                .map(s -> resultOf(s, false))
                .toList();
    }

    /** One attempt's result. Teachers always see the correct answers. */
    @Transactional(readOnly = true)
    public QuizAttemptResult result(Long submissionId, boolean teacherView) {
        Submission submission = submissionService.get(submissionId);
        if (!(submission.getContent() instanceof Quiz)) {
            throw new IllegalArgumentException("Submission " + submissionId + " is not a quiz attempt");
        }
        return resultOf(submission, teacherView);
    }

    private QuizAttemptResult resultOf(Submission submission, boolean teacherView) {
        Quiz quiz = (Quiz) submission.getContent();
        List<Submission> studentsAttempts = submissions.findByContentIdAndSubmittedByIdOrderBySubmittedAtDescIdDesc(
                quiz.getId(), submission.getSubmittedBy().getId());
        int attemptsUsed = studentsAttempts.size();
        int attemptNumber = attemptsUsed - indexOf(studentsAttempts, submission);
        boolean noAttemptsLeft = quiz.getMaxAttempts() != null && attemptsUsed >= quiz.getMaxAttempts();
        boolean showCorrect = teacherView || noAttemptsLeft;
        boolean showHints = teacherView || Boolean.TRUE.equals(quiz.getShowHints());

        List<QuizAttemptResult.QuestionResult> results = new ArrayList<>();
        double total = 0;
        for (QuizAnswer a : quizAnswers.findBySubmissionIdOrderByQuestionPositionAsc(submission.getId())) {
            Question q = a.getQuestion();
            total += a.getEarnedPoints();
            results.add(new QuizAttemptResult.QuestionResult(
                    toStudentView(q), a.getEarnedPoints(), a.isCorrect(), a.getAnswer(),
                    !a.isCorrect() && showHints ? q.getHint() : null,
                    showCorrect ? q.correctAnswer() : null));
        }
        return new QuizAttemptResult(submission.getId(), quiz.getId(), attemptNumber, attemptsUsed,
                quiz.getMaxAttempts(), submission.getSubmittedAt(), submission.isLate(), round(total),
                quiz.getMaxPoints(), showCorrect, results);
    }

    private StudentQuestion toStudentView(Question q) {
        return switch (q) {
            case MultipleChoiceQuestion mc -> new StudentQuestion(q.getId(), q.getPosition(), q.getType(),
                    q.getPrompt(), q.getPoints(), List.copyOf(mc.getOptions()), mc.multipleAnswers(),
                    null, null, null, null, null);
            case OrderingQuestion o -> new StudentQuestion(q.getId(), q.getPosition(), q.getType(),
                    q.getPrompt(), q.getPoints(), null, null, shuffled(o.getItems()), null, null, null, null);
            case MatchingQuestion m -> new StudentQuestion(q.getId(), q.getPosition(), q.getType(),
                    q.getPrompt(), q.getPoints(), null, null, null,
                    m.getPairs().stream().map(MatchPair::getLeft).toList(),
                    shuffled(m.getPairs().stream().map(MatchPair::getRight).distinct().toList()), null, null);
            case FillInBlanksQuestion f -> new StudentQuestion(q.getId(), q.getPosition(), q.getType(),
                    q.getPrompt(), q.getPoints(), null, null, null, null, null,
                    f.textParts(), f.textParts().size() - 1);
            default -> throw new IllegalStateException("Unknown question type " + q.getType());
        };
    }

    /** A shuffled copy that (for 2+ different items) is not already in the original order. */
    private List<String> shuffled(List<String> original) {
        List<String> copy = new ArrayList<>(original);
        if (new HashSet<>(copy).size() < 2) {
            return copy;
        }
        do {
            Collections.shuffle(copy, random);
        } while (copy.equals(original));
        return copy;
    }

    private static int indexOf(List<Submission> list, Submission s) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getId().equals(s.getId())) {
                return i;
            }
        }
        return 0;
    }

    private static double round(double value) {
        return Math.round(value * 100) / 100.0;
    }
}
