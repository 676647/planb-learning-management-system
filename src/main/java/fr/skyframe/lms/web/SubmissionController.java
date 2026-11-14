package fr.skyframe.lms.web;

import fr.skyframe.lms.security.CurrentUser;
import fr.skyframe.lms.service.GradingService;
import fr.skyframe.lms.service.SubmissionService;
import fr.skyframe.lms.service.UploadedFile;
import fr.skyframe.lms.web.dto.GradeResponse;
import fr.skyframe.lms.web.dto.SubmissionRequest;
import fr.skyframe.lms.web.dto.SubmissionResponse;
import fr.skyframe.lms.web.dto.SubmissionWithGradeResponse;
import jakarta.validation.Valid;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

/** Students hand in work for quizzes, coding exercises and assignments (US2). */
@RestController
@RequestMapping("/api")
public class SubmissionController {

    private final SubmissionService submissionService;
    private final GradingService gradingService;
    private final CurrentUser currentUser;

    public SubmissionController(SubmissionService submissionService, GradingService gradingService,
                                CurrentUser currentUser) {
        this.submissionService = submissionService;
        this.gradingService = gradingService;
        this.currentUser = currentUser;
    }

    /** The logged-in user hands in a text answer (JSON). SubmissionService checks they are a student of the course. */
    @PostMapping(path = "/contents/{contentId}/submissions", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("@access.isMemberOfContent(#contentId)")
    @ResponseStatus(HttpStatus.CREATED)
    public SubmissionResponse submit(@PathVariable Long contentId, @Valid @RequestBody SubmissionRequest request) {
        return SubmissionResponse.from(submissionService.submit(contentId, currentUser.id(), request.answer()));
    }

    /**
     * Hands in with a file (multipart/form-data, as sent by an HTML file input):
     * an optional "answer" text field and an optional "file" part. At least one is required.
     */
    @PostMapping(path = "/contents/{contentId}/submissions", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@access.isMemberOfContent(#contentId)")
    @ResponseStatus(HttpStatus.CREATED)
    public SubmissionResponse submitWithFile(@PathVariable Long contentId,
                                             @RequestParam(required = false) String answer,
                                             @RequestPart(required = false) MultipartFile file) {
        UploadedFile upload = file == null || file.isEmpty() ? null
                : new UploadedFile(file.getOriginalFilename(), file.getContentType(), file.getSize(), file);
        return SubmissionResponse.from(submissionService.submit(contentId, currentUser.id(), answer, upload));
    }

    /**
     * Downloads the file of a submission. Allowed for the course's teachers and for the student
     * (or group) it belongs to. Always sent as a download ("attachment"), never shown inline,
     * so an uploaded file can't run as a web page in our site.
     */
    @GetMapping("/submissions/{id}/file")
    @PreAuthorize("@access.canSeeSubmission(#id)")
    public ResponseEntity<InputStreamResource> downloadFile(@PathVariable Long id) {
        SubmissionService.SubmissionFile file = submissionService.openFile(id);
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(file.reference().getOriginalFilename(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(mediaTypeOf(file.reference().getContentType()))
                .contentLength(file.reference().getSize())
                .body(new InputStreamResource(file.content()));
    }

    /** All submissions for an item with their grades, newest first (the teacher's grading queue). */
    @GetMapping("/contents/{contentId}/submissions")
    @PreAuthorize("@access.isTeacherOfContent(#contentId)")
    public List<SubmissionWithGradeResponse> list(@PathVariable Long contentId) {
        return submissionService.listForContent(contentId).stream()
                .map(s -> new SubmissionWithGradeResponse(
                        SubmissionResponse.from(s),
                        gradingService.gradeOf(s.getId()).map(GradeResponse::from).orElse(null)))
                .toList();
    }

    @GetMapping("/submissions/{id}")
    @PreAuthorize("@access.canSeeSubmission(#id)")
    public SubmissionResponse get(@PathVariable Long id) {
        return SubmissionResponse.from(submissionService.get(id));
    }

    /** The content type stored at upload came from the browser; fall back if it is not valid. */
    private static MediaType mediaTypeOf(String contentType) {
        try {
            return MediaType.parseMediaType(contentType);
        } catch (RuntimeException e) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }
}
