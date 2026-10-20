package fr.skyframe.lms.web;

import fr.skyframe.lms.service.ContentService;
import fr.skyframe.lms.web.dto.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API for the content items of a module.
 * Each content type has its own create endpoint, because each takes different fields.
 * Listing, reading, reordering and deleting work the same for all types.
 */
@RestController
@RequestMapping("/api")
public class ContentController {

    private final ContentService contentService;

    public ContentController(ContentService contentService) {
        this.contentService = contentService;
    }

    @PostMapping("/modules/{moduleId}/pages")
    @ResponseStatus(HttpStatus.CREATED)
    public ContentResponse addPage(@PathVariable Long moduleId, @Valid @RequestBody PageRequest request) {
        return ContentResponse.from(contentService.add(moduleId, request.toEntity()));
    }

    @PostMapping("/modules/{moduleId}/quizzes")
    @ResponseStatus(HttpStatus.CREATED)
    public ContentResponse addQuiz(@PathVariable Long moduleId, @Valid @RequestBody QuizRequest request) {
        return ContentResponse.from(contentService.add(moduleId, request.toEntity()));
    }

    @PostMapping("/modules/{moduleId}/coding-exercises")
    @ResponseStatus(HttpStatus.CREATED)
    public ContentResponse addCodingExercise(@PathVariable Long moduleId,
                                             @Valid @RequestBody CodingExerciseRequest request) {
        return ContentResponse.from(contentService.add(moduleId, request.toEntity()));
    }

    @PostMapping("/modules/{moduleId}/assignments")
    @ResponseStatus(HttpStatus.CREATED)
    public ContentResponse addAssignment(@PathVariable Long moduleId, @Valid @RequestBody AssignmentRequest request) {
        return ContentResponse.from(contentService.add(moduleId, request.toEntity()));
    }

    /**
     * Lists a module's contents in order.
     * With ?visibleOnly=true only items inside their visibility window are returned (the student's view).
     */
    @GetMapping("/modules/{moduleId}/contents")
    public List<ContentResponse> list(@PathVariable Long moduleId,
                                      @RequestParam(defaultValue = "false") boolean visibleOnly) {
        var items = visibleOnly
                ? contentService.listVisibleForModule(moduleId)
                : contentService.listForModule(moduleId);
        return items.stream().map(ContentResponse::from).toList();
    }

    @PutMapping("/modules/{moduleId}/contents/order")
    public List<ContentResponse> reorder(@PathVariable Long moduleId, @Valid @RequestBody OrderRequest request) {
        return contentService.reorder(moduleId, request.ids()).stream().map(ContentResponse::from).toList();
    }

    @GetMapping("/contents/{id}")
    public ContentResponse get(@PathVariable Long id) {
        return ContentResponse.from(contentService.get(id));
    }

    @DeleteMapping("/contents/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        contentService.delete(id);
    }
}
