package fr.skyframe.lms.service;

import fr.skyframe.lms.domain.Content;
import fr.skyframe.lms.domain.CourseModule;
import fr.skyframe.lms.domain.GradedContent;
import fr.skyframe.lms.domain.Quiz;
import fr.skyframe.lms.repository.ContentRepository;
import fr.skyframe.lms.repository.GradeRepository;
import fr.skyframe.lms.repository.SubmissionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Business logic for the content items (pages, quizzes, exercises, assignments) of a module. */
@Service
@Transactional
public class ContentService {

    private final ContentRepository contents;
    private final SubmissionRepository submissions;
    private final GradeRepository grades;
    private final ModuleService moduleService;
    private final Clock clock;

    public ContentService(ContentRepository contents, SubmissionRepository submissions, GradeRepository grades,
                          ModuleService moduleService, Clock clock) {
        this.contents = contents;
        this.submissions = submissions;
        this.grades = grades;
        this.moduleService = moduleService;
        this.clock = clock;
    }

    /**
     * Adds a content item (of any type) at the end of a module.
     * The caller creates the Page/Quiz/... object; this method validates it and stores it.
     */
    public <T extends Content> T add(Long moduleId, T content) {
        CourseModule module = moduleService.get(moduleId);
        validate(content);
        int nextPosition = contents.findTopByModuleIdOrderByPositionDesc(moduleId)
                .map(last -> last.getPosition() + 1)
                .orElse(0);
        content.setModule(module);
        content.setPosition(nextPosition);
        return contents.save(content);
    }

    @Transactional(readOnly = true)
    public Content get(Long id) {
        return contents.findById(id)
                .orElseThrow(() -> new NotFoundException("Content " + id + " not found"));
    }

    /** Like get(), but only for gradable items (quizzes, coding exercises, assignments). */
    @Transactional(readOnly = true)
    public GradedContent getGraded(Long id) {
        if (get(id) instanceof GradedContent graded) {
            return graded;
        }
        throw new IllegalArgumentException("Content " + id + " is a page and cannot be submitted or graded");
    }

    /** All content items of a module in order (the teacher's view). */
    @Transactional(readOnly = true)
    public List<Content> listForModule(Long moduleId) {
        moduleService.get(moduleId); // 404 if the module does not exist
        return contents.findByModuleIdOrderByPositionAsc(moduleId);
    }

    /** Only the items that are inside their visibility window right now (the student's view). */
    @Transactional(readOnly = true)
    public List<Content> listVisibleForModule(Long moduleId) {
        Instant now = clock.instant();
        return listForModule(moduleId).stream()
                .filter(c -> c.isVisibleAt(now))
                .toList();
    }

    /** Puts the module's contents in the given order. The list must contain every item exactly once. */
    public List<Content> reorder(Long moduleId, List<Long> orderedIds) {
        List<Content> current = listForModule(moduleId);
        Set<Long> currentIds = new HashSet<>();
        current.forEach(c -> currentIds.add(c.getId()));
        if (orderedIds.size() != current.size() || !currentIds.equals(new HashSet<>(orderedIds))) {
            throw new IllegalArgumentException("The new order must list every item of module " + moduleId + " exactly once");
        }
        for (int i = 0; i < orderedIds.size(); i++) {
            Long id = orderedIds.get(i);
            int position = i;
            current.stream().filter(c -> c.getId().equals(id)).findFirst()
                    .ifPresent(c -> c.setPosition(position));
        }
        return contents.findByModuleIdOrderByPositionAsc(moduleId);
    }

    /** Deletes an item together with its submissions and grades. */
    public void delete(Long id) {
        Content content = get(id);
        grades.deleteBySubmissionContentId(id);
        submissions.deleteByContentId(id);
        contents.delete(content);
    }

    private void validate(Content content) {
        if (content.getTitle() == null || content.getTitle().isBlank()) {
            throw new IllegalArgumentException("Title is required");
        }
        if (content.getVisibleFrom() != null && content.getVisibleUntil() != null
                && !content.getVisibleUntil().isAfter(content.getVisibleFrom())) {
            throw new IllegalArgumentException("visibleUntil must be after visibleFrom");
        }
        if (content instanceof Quiz quiz && quiz.getMaxAttempts() != null && quiz.getMaxAttempts() < 1) {
            throw new IllegalArgumentException("A quiz must allow at least one attempt");
        }
    }
}
