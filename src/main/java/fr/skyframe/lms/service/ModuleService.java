package fr.skyframe.lms.service;

import fr.skyframe.lms.domain.Course;
import fr.skyframe.lms.domain.CourseModule;
import fr.skyframe.lms.repository.ContentRepository;
import fr.skyframe.lms.repository.CourseModuleRepository;
import fr.skyframe.lms.repository.CourseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Business logic for the modules of a course: ordering and dependencies between modules. */
@Service
@Transactional
public class ModuleService {

    private final CourseModuleRepository modules;
    private final ContentRepository contents;
    private final CourseRepository courses;
    private final ContentDataCleaner cleaner;

    public ModuleService(CourseModuleRepository modules, ContentRepository contents, CourseRepository courses,
                         ContentDataCleaner cleaner) {
        this.modules = modules;
        this.contents = contents;
        this.courses = courses;
        this.cleaner = cleaner;
    }

    /** Adds a new module at the end of the course. */
    public CourseModule create(Long courseId, String title) {
        Course course = courses.findById(courseId)
                .orElseThrow(() -> new NotFoundException("Course " + courseId + " not found"));
        int nextPosition = modules.findTopByCourseIdOrderByPositionDesc(courseId)
                .map(last -> last.getPosition() + 1)
                .orElse(0);

        CourseModule module = new CourseModule();
        module.setCourse(course);
        module.setTitle(title);
        module.setPosition(nextPosition);
        return modules.save(module);
    }

    @Transactional(readOnly = true)
    public CourseModule get(Long id) {
        return modules.findById(id)
                .orElseThrow(() -> new NotFoundException("Module " + id + " not found"));
    }

    /** The modules of a course, in order. */
    @Transactional(readOnly = true)
    public List<CourseModule> listForCourse(Long courseId) {
        if (!courses.existsById(courseId)) {
            throw new NotFoundException("Course " + courseId + " not found");
        }
        return modules.findByCourseIdOrderByPositionAsc(courseId);
    }

    public CourseModule rename(Long id, String title) {
        CourseModule module = get(id);
        module.setTitle(title);
        return module;
    }

    /**
     * Puts the course's modules in the given order. The list must contain every module of
     * the course exactly once.
     */
    public List<CourseModule> reorder(Long courseId, List<Long> orderedIds) {
        List<CourseModule> current = listForCourse(courseId);
        Set<Long> currentIds = new HashSet<>();
        current.forEach(m -> currentIds.add(m.getId()));
        if (orderedIds.size() != current.size() || !currentIds.equals(new HashSet<>(orderedIds))) {
            throw new IllegalArgumentException("The new order must list every module of course " + courseId + " exactly once");
        }
        for (int i = 0; i < orderedIds.size(); i++) {
            Long id = orderedIds.get(i);
            int position = i;
            current.stream().filter(m -> m.getId().equals(id)).findFirst()
                    .ifPresent(m -> m.setPosition(position));
        }
        return modules.findByCourseIdOrderByPositionAsc(courseId);
    }

    /** Makes module {@code moduleId} require module {@code requiredId}. */
    public CourseModule addRequirement(Long moduleId, Long requiredId) {
        CourseModule module = get(moduleId);
        CourseModule required = get(requiredId);
        if (module.getId().equals(required.getId())) {
            throw new IllegalArgumentException("A module cannot require itself");
        }
        if (!module.getCourse().getId().equals(required.getCourse().getId())) {
            throw new IllegalArgumentException("A module can only require modules of the same course");
        }
        if (dependsOn(required, module)) {
            throw new ConflictException("Module " + requiredId + " already depends on module " + moduleId
                    + "; adding this requirement would create a cycle");
        }
        module.getRequires().add(required);
        return module;
    }

    public CourseModule removeRequirement(Long moduleId, Long requiredId) {
        CourseModule module = get(moduleId);
        module.getRequires().removeIf(m -> m.getId().equals(requiredId));
        return module;
    }

    /** Deletes a module together with its contents and any dependencies on it. */
    public void delete(Long id) {
        CourseModule module = get(id);
        for (CourseModule dependent : modules.findByRequiresId(id)) {
            dependent.getRequires().remove(module);
        }
        module.getRequires().clear();
        deleteContentsOf(id);
        modules.delete(module);
    }

    /** Deletes every module of a course. Called by CourseService when a course is deleted. */
    public void deleteAllOfCourse(Long courseId) {
        List<CourseModule> all = modules.findByCourseIdOrderByPositionAsc(courseId);
        // Dependencies only exist within a course, so clearing them all first is enough.
        all.forEach(m -> m.getRequires().clear());
        for (CourseModule module : all) {
            deleteContentsOf(module.getId());
            modules.delete(module);
        }
    }

    /** Deletes a module's contents, first removing everything that points to them. */
    private void deleteContentsOf(Long moduleId) {
        for (var content : contents.findByModuleIdOrderByPositionAsc(moduleId)) {
            cleaner.deleteDataOf(content.getId());
        }
        contents.deleteByModuleId(moduleId);
    }

    /** True if {@code from} requires {@code target}, directly or through other modules. */
    private boolean dependsOn(CourseModule from, CourseModule target) {
        Deque<CourseModule> toVisit = new ArrayDeque<>(from.getRequires());
        Set<Long> visited = new HashSet<>();
        while (!toVisit.isEmpty()) {
            CourseModule next = toVisit.pop();
            if (next.getId().equals(target.getId())) {
                return true;
            }
            if (visited.add(next.getId())) {
                toVisit.addAll(next.getRequires());
            }
        }
        return false;
    }
}
