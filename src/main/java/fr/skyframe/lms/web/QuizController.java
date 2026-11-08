package fr.skyframe.lms.web;

import fr.skyframe.lms.security.CourseAccess;
import fr.skyframe.lms.security.CurrentUser;
import fr.skyframe.lms.service.*;
import fr.skyframe.lms.web.dto.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Quizzes: teachers manage the questions; students get the questions without solutions,
 * answer them in attempts and see their results right away.
 */
@RestController
@RequestMapping("/api")
public class QuizController {

    private final QuizService quizService;
    private final ContentService contentService;
    private final CourseAccess access;
    private final CurrentUser currentUser;

    public QuizController(QuizService quizService, ContentService contentService, CourseAccess access,
                          CurrentUser currentUser) {
        this.quizService = quizService;
        this.contentService = contentService;
        this.access = access;
        this.currentUser = currentUser;
    }

    // --- questions, for teachers (with solutions) ---

    @GetMapping("/contents/{quizId}/questions")
    @PreAuthorize("@access.isTeacherOfContent(#quizId)")
    public List<QuestionResponse> questions(@PathVariable Long quizId) {
        return quizService.questionsOf(quizId).stream().map(QuestionResponse::from).toList();
    }

    @PostMapping("/contents/{quizId}/questions/multiple-choice")
    @PreAuthorize("@access.isTeacherOfContent(#quizId)")
    @ResponseStatus(HttpStatus.CREATED)
    public QuestionResponse addMultipleChoice(@PathVariable Long quizId,
                                              @Valid @RequestBody MultipleChoiceQuestionRequest request) {
        return QuestionResponse.from(quizService.addQuestion(quizId, request.toEntity()));
    }

    @PostMapping("/contents/{quizId}/questions/ordering")
    @PreAuthorize("@access.isTeacherOfContent(#quizId)")
    @ResponseStatus(HttpStatus.CREATED)
    public QuestionResponse addOrdering(@PathVariable Long quizId, @Valid @RequestBody OrderingQuestionRequest request) {
        return QuestionResponse.from(quizService.addQuestion(quizId, request.toEntity()));
    }

    @PostMapping("/contents/{quizId}/questions/matching")
    @PreAuthorize("@access.isTeacherOfContent(#quizId)")
    @ResponseStatus(HttpStatus.CREATED)
    public QuestionResponse addMatching(@PathVariable Long quizId, @Valid @RequestBody MatchingQuestionRequest request) {
        return QuestionResponse.from(quizService.addQuestion(quizId, request.toEntity()));
    }

    @PostMapping("/contents/{quizId}/questions/fill-in-blanks")
    @PreAuthorize("@access.isTeacherOfContent(#quizId)")
    @ResponseStatus(HttpStatus.CREATED)
    public QuestionResponse addFillInBlanks(@PathVariable Long quizId,
                                            @Valid @RequestBody FillInBlanksQuestionRequest request) {
        return QuestionResponse.from(quizService.addQuestion(quizId, request.toEntity()));
    }

    @DeleteMapping("/questions/{id}")
    @PreAuthorize("@access.isTeacherOfQuestion(#id)")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteQuestion(@PathVariable Long id) {
        quizService.deleteQuestion(id);
    }

    // --- answering, for students ---

    /** The questions without solutions. Teachers can use it to preview what students see. */
    @GetMapping("/contents/{quizId}/questions/student-view")
    @PreAuthorize("@access.isMemberOfContent(#quizId)")
    public List<StudentQuestion> studentView(@PathVariable Long quizId) {
        if (!access.isTeacherOfContent(quizId) && !contentService.isVisibleNow(quizService.getQuiz(quizId))) {
            throw new NotFoundException("Content " + quizId + " not found");
        }
        return quizService.studentView(quizId);
    }

    /** The logged-in student answers the quiz; the graded result comes back immediately. */
    @PostMapping("/contents/{quizId}/attempts")
    @PreAuthorize("@access.isMemberOfContent(#quizId)")
    @ResponseStatus(HttpStatus.CREATED)
    public QuizAttemptResult attempt(@PathVariable Long quizId, @Valid @RequestBody QuizAttemptRequest request) {
        return quizService.attempt(quizId, currentUser.id(), request.answers());
    }

    /** The logged-in student's own attempts, newest first. */
    @GetMapping("/contents/{quizId}/attempts/mine")
    @PreAuthorize("@access.isMemberOfContent(#quizId)")
    public List<QuizAttemptResult> myAttempts(@PathVariable Long quizId) {
        return quizService.attemptsOf(quizId, currentUser.id());
    }

    /** One attempt in detail: for the student it belongs to, or a teacher (who also sees the solutions). */
    @GetMapping("/submissions/{id}/quiz-result")
    @PreAuthorize("@access.canSeeSubmission(#id)")
    public QuizAttemptResult attemptResult(@PathVariable Long id) {
        return quizService.result(id, access.isTeacherOfSubmission(id));
    }
}
