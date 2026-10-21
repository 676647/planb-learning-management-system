package fr.skyframe.lms.web;

import fr.skyframe.lms.service.Gradebook;
import fr.skyframe.lms.service.GradingService;
import fr.skyframe.lms.service.NotFoundException;
import fr.skyframe.lms.service.StudentGrade;
import fr.skyframe.lms.web.dto.ContentResponse;
import fr.skyframe.lms.web.dto.GradeRequest;
import fr.skyframe.lms.web.dto.GradeResponse;
import fr.skyframe.lms.web.dto.PublishGradesRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** US3: grading, publishing grades and following student progress. */
@RestController
@RequestMapping("/api")
public class GradingController {

    private final GradingService gradingService;

    public GradingController(GradingService gradingService) {
        this.gradingService = gradingService;
    }

    /** Grades a submission (or changes its grade). */
    @PutMapping("/submissions/{id}/grade")
    public GradeResponse grade(@PathVariable Long id, @Valid @RequestBody GradeRequest request) {
        return GradeResponse.from(gradingService.grade(id, request.teacherId(), request.points(), request.feedback()));
    }

    @GetMapping("/submissions/{id}/grade")
    public GradeResponse getGrade(@PathVariable Long id) {
        return gradingService.gradeOf(id).map(GradeResponse::from)
                .orElseThrow(() -> new NotFoundException("Submission " + id + " has not been graded yet"));
    }

    /** Makes an item's grades visible to students ({"published": true}) or hides them again. */
    @PutMapping("/contents/{contentId}/grades-published")
    public ContentResponse publish(@PathVariable Long contentId, @Valid @RequestBody PublishGradesRequest request) {
        return ContentResponse.from(gradingService.setGradesPublished(contentId, request.teacherId(), request.published()));
    }

    /** The teacher's overview: all students against all gradable items of a course. */
    @GetMapping("/courses/{courseId}/gradebook")
    public Gradebook gradebook(@PathVariable Long courseId) {
        return gradingService.gradebook(courseId);
    }

    /** A student's own results in a course. Unpublished grades are hidden. */
    @GetMapping("/courses/{courseId}/students/{studentId}/grades")
    public List<StudentGrade> studentGrades(@PathVariable Long courseId, @PathVariable Long studentId) {
        return gradingService.gradesOfStudent(courseId, studentId);
    }
}
