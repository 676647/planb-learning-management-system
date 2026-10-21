package fr.skyframe.lms.web;

import fr.skyframe.lms.security.CurrentUser;
import fr.skyframe.lms.service.SubmissionService;
import fr.skyframe.lms.web.dto.SubmissionRequest;
import fr.skyframe.lms.web.dto.SubmissionResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Students hand in work for quizzes, coding exercises and assignments (US2). */
@RestController
@RequestMapping("/api")
public class SubmissionController {

    private final SubmissionService submissionService;
    private final CurrentUser currentUser;

    public SubmissionController(SubmissionService submissionService, CurrentUser currentUser) {
        this.submissionService = submissionService;
        this.currentUser = currentUser;
    }

    /** The logged-in user hands in. SubmissionService checks they are a student of the course. */
    @PostMapping("/contents/{contentId}/submissions")
    @PreAuthorize("@access.isMemberOfContent(#contentId)")
    @ResponseStatus(HttpStatus.CREATED)
    public SubmissionResponse submit(@PathVariable Long contentId, @Valid @RequestBody SubmissionRequest request) {
        return SubmissionResponse.from(submissionService.submit(contentId, currentUser.id(), request.answer()));
    }

    /** All submissions for an item, newest first (the teacher's grading queue). */
    @GetMapping("/contents/{contentId}/submissions")
    @PreAuthorize("@access.isTeacherOfContent(#contentId)")
    public List<SubmissionResponse> list(@PathVariable Long contentId) {
        return submissionService.listForContent(contentId).stream().map(SubmissionResponse::from).toList();
    }

    @GetMapping("/submissions/{id}")
    @PreAuthorize("@access.canSeeSubmission(#id)")
    public SubmissionResponse get(@PathVariable Long id) {
        return SubmissionResponse.from(submissionService.get(id));
    }
}
