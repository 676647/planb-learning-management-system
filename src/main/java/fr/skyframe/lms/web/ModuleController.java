package fr.skyframe.lms.web;

import fr.skyframe.lms.service.ModuleService;
import fr.skyframe.lms.web.dto.ModuleRequest;
import fr.skyframe.lms.web.dto.ModuleResponse;
import fr.skyframe.lms.web.dto.OrderRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API for the modules of a course (part of US1: teachers design a course's structure).
 * Teachers of the course can change modules; all participants can read them.
 */
@RestController
@RequestMapping("/api")
public class ModuleController {

    private final ModuleService moduleService;

    public ModuleController(ModuleService moduleService) {
        this.moduleService = moduleService;
    }

    @PostMapping("/courses/{courseId}/modules")
    @PreAuthorize("@access.isTeacherOfCourse(#courseId)")
    @ResponseStatus(HttpStatus.CREATED)
    public ModuleResponse create(@PathVariable Long courseId, @Valid @RequestBody ModuleRequest request) {
        return ModuleResponse.from(moduleService.create(courseId, request.title()));
    }

    @GetMapping("/courses/{courseId}/modules")
    @PreAuthorize("@access.isMemberOfCourse(#courseId)")
    public List<ModuleResponse> list(@PathVariable Long courseId) {
        return moduleService.listForCourse(courseId).stream().map(ModuleResponse::from).toList();
    }

    @PutMapping("/courses/{courseId}/modules/order")
    @PreAuthorize("@access.isTeacherOfCourse(#courseId)")
    public List<ModuleResponse> reorder(@PathVariable Long courseId, @Valid @RequestBody OrderRequest request) {
        return moduleService.reorder(courseId, request.ids()).stream().map(ModuleResponse::from).toList();
    }

    @GetMapping("/modules/{id}")
    @PreAuthorize("@access.isMemberOfModule(#id)")
    public ModuleResponse get(@PathVariable Long id) {
        return ModuleResponse.from(moduleService.get(id));
    }

    @PutMapping("/modules/{id}")
    @PreAuthorize("@access.isTeacherOfModule(#id)")
    public ModuleResponse rename(@PathVariable Long id, @Valid @RequestBody ModuleRequest request) {
        return ModuleResponse.from(moduleService.rename(id, request.title()));
    }

    @DeleteMapping("/modules/{id}")
    @PreAuthorize("@access.isTeacherOfModule(#id)")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        moduleService.delete(id);
    }

    /** Module {id} now requires module {requiredId} to be completed first. */
    @PutMapping("/modules/{id}/requires/{requiredId}")
    @PreAuthorize("@access.isTeacherOfModule(#id)")
    public ModuleResponse addRequirement(@PathVariable Long id, @PathVariable Long requiredId) {
        return ModuleResponse.from(moduleService.addRequirement(id, requiredId));
    }

    @DeleteMapping("/modules/{id}/requires/{requiredId}")
    @PreAuthorize("@access.isTeacherOfModule(#id)")
    public ModuleResponse removeRequirement(@PathVariable Long id, @PathVariable Long requiredId) {
        return ModuleResponse.from(moduleService.removeRequirement(id, requiredId));
    }
}
