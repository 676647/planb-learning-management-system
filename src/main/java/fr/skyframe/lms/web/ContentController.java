package fr.skyframe.lms.web;

import fr.skyframe.lms.domain.Content;
import fr.skyframe.lms.security.CourseAccess;
import fr.skyframe.lms.service.ContentService;
import fr.skyframe.lms.service.NotFoundException;
import fr.skyframe.lms.web.dto.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API for the content items of a module.
 * Teachers of the course create, reorder and delete items. Students only ever see items
 * that are inside their visibility window.
 */
@RestController
@RequestMapping("/api")
public class ContentController {

    private final ContentService contentService;
    private final CourseAccess access;

    public ContentController(ContentService contentService, CourseAccess access) {
        this.contentService = contentService;
        this.access = access;
    }

    @PostMapping("/modules/{moduleId}/pages")
    @PreAuthorize("@access.isTeacherOfModule(#moduleId)")
    @ResponseStatus(HttpStatus.CREATED)
    public ContentResponse addPage(@PathVariable Long moduleId, @Valid @RequestBody PageRequest request) {
        return ContentResponse.from(contentService.add(moduleId, request.toEntity()));
    }

    @PostMapping("/modules/{moduleId}/quizzes")
    @PreAuthorize("@access.isTeacherOfModule(#moduleId)")
    @ResponseStatus(HttpStatus.CREATED)
    public ContentResponse addQuiz(@PathVariable Long moduleId, @Valid @RequestBody QuizRequest request) {
        return ContentResponse.from(contentService.add(moduleId, request.toEntity()));
    }

    @PostMapping("/modules/{moduleId}/coding-exercises")
    @PreAuthorize("@access.isTeacherOfModule(#moduleId)")
    @ResponseStatus(HttpStatus.CREATED)
    public ContentResponse addCodingExercise(@PathVariable Long moduleId,
                                             @Valid @RequestBody CodingExerciseRequest request) {
        return ContentResponse.from(contentService.add(moduleId, request.toEntity()));
    }

    @PostMapping("/modules/{moduleId}/assignments")
    @PreAuthorize("@access.isTeacherOfModule(#moduleId)")
    @ResponseStatus(HttpStatus.CREATED)
    public ContentResponse addAssignment(@PathVariable Long moduleId, @Valid @RequestBody AssignmentRequest request) {
        return ContentResponse.from(contentService.add(moduleId, request.toEntity()));
    }

    /**
     * Lists a module's contents in order. Teachers see everything (or only visible items with
     * ?visibleOnly=true, to preview the student view); students always get only visible items.
     */
    @GetMapping("/modules/{moduleId}/contents")
    @PreAuthorize("@access.isMemberOfModule(#moduleId)")
    public List<ContentResponse> list(@PathVariable Long moduleId,
                                      @RequestParam(defaultValue = "false") boolean visibleOnly) {
        boolean onlyVisible = visibleOnly || !access.isTeacherOfModule(moduleId);
        var items = onlyVisible
                ? contentService.listVisibleForModule(moduleId)
                : contentService.listForModule(moduleId);
        return items.stream().map(ContentResponse::from).toList();
    }

    @PutMapping("/modules/{moduleId}/contents/order")
    @PreAuthorize("@access.isTeacherOfModule(#moduleId)")
    public List<ContentResponse> reorder(@PathVariable Long moduleId, @Valid @RequestBody OrderRequest request) {
        return contentService.reorder(moduleId, request.ids()).stream().map(ContentResponse::from).toList();
    }

    /** Students get 404 for items that are not visible yet, as if they did not exist. */
    @GetMapping("/contents/{id}")
    @PreAuthorize("@access.isMemberOfContent(#id)")
    public ContentResponse get(@PathVariable Long id) {
        Content content = contentService.get(id);
        if (!access.isTeacherOfContent(id) && !contentService.isVisibleNow(content)) {
            throw new NotFoundException("Content " + id + " not found");
        }
        return ContentResponse.from(content);
    }

    @DeleteMapping("/contents/{id}")
    @PreAuthorize("@access.isTeacherOfContent(#id)")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        contentService.delete(id);
    }
}
