package fr.skyframe.lms.web;

import fr.skyframe.lms.security.CurrentUser;
import fr.skyframe.lms.service.Gradebook;
import fr.skyframe.lms.service.GradingService;
import fr.skyframe.lms.service.NotFoundException;
import fr.skyframe.lms.service.StudentGrade;
import fr.skyframe.lms.web.dto.ContentResponse;
import fr.skyframe.lms.web.dto.GradeRequest;
import fr.skyframe.lms.web.dto.GradeResponse;
import fr.skyframe.lms.web.dto.PublishGradesRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** US3: grading, publishing grades and following student progress. */
@RestController
@RequestMapping("/api")
public class GradingController {

    private final GradingService gradingService;
    private final CurrentUser currentUser;

    public GradingController(GradingService gradingService, CurrentUser currentUser) {
        this.gradingService = gradingService;
        this.currentUser = currentUser;
    }

    /** Grades a submission (or changes its grade). */
    @PutMapping("/submissions/{id}/grade")
    @PreAuthorize("@access.isTeacherOfSubmission(#id)")
    public GradeResponse grade(@PathVariable Long id, @Valid @RequestBody GradeRequest request) {
        return GradeResponse.from(gradingService.grade(id, currentUser.id(), request.points(), request.feedback()));
    }

    /** Teachers can always read a grade; students only their own, once published. */
    @GetMapping("/submissions/{id}/grade")
    @PreAuthorize("@access.canSeeGrade(#id)")
    public GradeResponse getGrade(@PathVariable Long id) {
        return gradingService.gradeOf(id).map(GradeResponse::from)
                .orElseThrow(() -> new NotFoundException("Submission " + id + " has not been graded yet"));
    }

    /** Makes an item's grades visible to students ({"published": true}) or hides them again. */
    @PutMapping("/contents/{contentId}/grades-published")
    @PreAuthorize("@access.isTeacherOfContent(#contentId)")
    public ContentResponse publish(@PathVariable Long contentId, @Valid @RequestBody PublishGradesRequest request) {
        return ContentResponse.from(
                gradingService.setGradesPublished(contentId, currentUser.id(), request.published()));
    }

    /** The teacher's overview: all students against all gradable items of a course. */
    @GetMapping("/courses/{courseId}/gradebook")
    @PreAuthorize("@access.isTeacherOfCourse(#courseId)")
    public Gradebook gradebook(@PathVariable Long courseId) {
        return gradingService.gradebook(courseId);
    }

    /** The logged-in student's own results in a course. Unpublished grades are hidden. */
    @GetMapping("/courses/{courseId}/my-grades")
    @PreAuthorize("@access.isMemberOfCourse(#courseId)")
    public List<StudentGrade> myGrades(@PathVariable Long courseId) {
        return gradingService.gradesOfStudent(courseId, currentUser.id());
    }

    /** A teacher looking at one student's results the way the student sees them. */
    @GetMapping("/courses/{courseId}/students/{studentId}/grades")
    @PreAuthorize("@access.isTeacherOfCourse(#courseId) or @access.isMe(#studentId)")
    public List<StudentGrade> studentGrades(@PathVariable Long courseId, @PathVariable Long studentId) {
        return gradingService.gradesOfStudent(courseId, studentId);
    }
}
