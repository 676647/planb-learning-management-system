package fr.skyframe.lms.service;

import fr.skyframe.lms.domain.*;
import fr.skyframe.lms.events.GradesPublished;
import fr.skyframe.lms.events.SubmissionGraded;
import fr.skyframe.lms.repository.EnrollmentRepository;
import org.springframework.context.ApplicationEventPublisher;
import fr.skyframe.lms.repository.GradeRepository;
import fr.skyframe.lms.repository.GradedContentRepository;
import fr.skyframe.lms.repository.SubmissionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * US3: teachers grade submissions, add feedback, decide when grades become visible,
 * and follow their students' progress in the gradebook.
 */
@Service
@Transactional
public class GradingService {

    private final GradeRepository grades;
    private final SubmissionRepository submissions;
    private final GradedContentRepository gradedContents;
    private final SubmissionService submissionService;
    private final ContentService contentService;
    private final UserService userService;
    private final EnrollmentRepository enrollments;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public GradingService(GradeRepository grades, SubmissionRepository submissions,
                          GradedContentRepository gradedContents, SubmissionService submissionService,
                          ContentService contentService, UserService userService,
                          EnrollmentRepository enrollments, ApplicationEventPublisher events, Clock clock) {
        this.grades = grades;
        this.submissions = submissions;
        this.gradedContents = gradedContents;
        this.submissionService = submissionService;
        this.contentService = contentService;
        this.userService = userService;
        this.enrollments = enrollments;
        this.events = events;
        this.clock = clock;
    }

    /** Grades a submission, or changes the existing grade. Only teachers of the course may do this. */
    public Grade grade(Long submissionId, Long teacherId, double points, String feedback) {
        Submission submission = submissionService.get(submissionId);
        GradedContent item = submission.getContent();
        User teacher = requireTeacher(item.getModule().getCourse().getId(), teacherId);

        if (points < 0 || (item.getMaxPoints() != null && points > item.getMaxPoints())) {
            throw new IllegalArgumentException("Points must be between 0 and "
                    + (item.getMaxPoints() != null ? item.getMaxPoints() : "the maximum"));
        }
        Grade grade = grades.findBySubmissionId(submissionId).orElseGet(Grade::new);
        grade.setSubmission(submission);
        grade.setPoints(points);
        grade.setFeedback(feedback);
        grade.setGradedBy(teacher);
        grade.setGradedAt(clock.instant());
        Grade saved = grades.save(grade);
        events.publishEvent(new SubmissionGraded(UUID.randomUUID(), saved.getGradedAt(), submissionId,
                item.getModule().getCourse().getId(), item.getId(), points, teacherId));
        return saved;
    }

    /** A grade given by the system (quizzes), not by a teacher. */
    public Grade autoGrade(Submission submission, double points, String feedback) {
        Grade grade = grades.findBySubmissionId(submission.getId()).orElseGet(Grade::new);
        grade.setSubmission(submission);
        grade.setPoints(points);
        grade.setFeedback(feedback);
        grade.setGradedBy(null);
        grade.setGradedAt(clock.instant());
        Grade saved = grades.save(grade);
        GradedContent item = submission.getContent();
        events.publishEvent(new SubmissionGraded(UUID.randomUUID(), saved.getGradedAt(), submission.getId(),
                item.getModule().getCourse().getId(), item.getId(), points, null));
        return saved;
    }

    @Transactional(readOnly = true)
    public Optional<Grade> gradeOf(Long submissionId) {
        return grades.findBySubmissionId(submissionId);
    }

    /** Makes the grades of an item visible (or hidden again) to students. */
    public GradedContent setGradesPublished(Long contentId, Long teacherId, boolean published) {
        GradedContent item = contentService.getGraded(contentId);
        requireTeacher(item.getModule().getCourse().getId(), teacherId);
        item.setGradesPublished(published);
        events.publishEvent(new GradesPublished(UUID.randomUUID(), clock.instant(),
                item.getModule().getCourse().getId(), item.getId(), item.getTitle(), published));
        return item;
    }

    /** The teacher's view: every student of the course against every gradable item. */
    @Transactional(readOnly = true)
    public Gradebook gradebook(Long courseId) {
        List<GradedContent> items = gradedContents.findByModuleCourseIdOrderByModulePositionAscPositionAsc(courseId);
        List<User> students = enrollments.findByCourseId(courseId).stream()
                .filter(e -> e.getRole() == Role.STUDENT)
                .map(Enrollment::getUser)
                .sorted(Comparator.comparing(User::getName, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
        List<Submission> allSubmissions = submissions.findByContentModuleCourseId(courseId);
        Map<Long, Grade> gradeBySubmission = grades.findBySubmissionContentModuleCourseId(courseId).stream()
                .collect(Collectors.toMap(g -> g.getSubmission().getId(), Function.identity()));

        List<Gradebook.StudentRow> rows = new ArrayList<>();
        for (User student : students) {
            List<Gradebook.Entry> entries = new ArrayList<>();
            double total = 0;
            for (GradedContent item : items) {
                Gradebook.Entry entry = entryFor(student, item, allSubmissions, gradeBySubmission);
                entries.add(entry);
                if (entry.points() != null) {
                    total += entry.points();
                }
            }
            rows.add(new Gradebook.StudentRow(student.getId(), student.getName(), student.getEmail(), entries, total));
        }

        List<Gradebook.Item> itemSummaries = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            GradedContent item = items.get(i);
            int index = i;
            List<Gradebook.Entry> column = rows.stream().map(r -> r.entries().get(index)).toList();
            int submitted = (int) column.stream().filter(e -> e.status() != Gradebook.Status.NOT_SUBMITTED).count();
            int graded = (int) column.stream().filter(e -> e.status() == Gradebook.Status.GRADED).count();
            Double average = column.stream().filter(e -> e.points() != null)
                    .mapToDouble(Gradebook.Entry::points).average().stream().boxed().findFirst().orElse(null);
            itemSummaries.add(new Gradebook.Item(item.getId(), item.getTitle(), item.getType(), item.getDeadline(),
                    item.getMaxPoints(), submitted, graded, average));
        }
        return new Gradebook(courseId, itemSummaries, rows);
    }

    /** The student's view of their own results. Grades of unpublished items stay hidden. */
    @Transactional(readOnly = true)
    public List<StudentGrade> gradesOfStudent(Long courseId, Long studentId) {
        User student = userService.get(studentId);
        if (!enrollments.existsByUserIdAndCourseIdAndRole(studentId, courseId, Role.STUDENT)) {
            throw new ForbiddenException("User " + studentId + " is not a student in course " + courseId);
        }
        List<Submission> allSubmissions = submissions.findByContentModuleCourseId(courseId);
        Map<Long, Grade> gradeBySubmission = grades.findBySubmissionContentModuleCourseId(courseId).stream()
                .collect(Collectors.toMap(g -> g.getSubmission().getId(), Function.identity()));

        List<StudentGrade> result = new ArrayList<>();
        for (GradedContent item : gradedContents.findByModuleCourseIdOrderByModulePositionAscPositionAsc(courseId)) {
            Gradebook.Entry entry = entryFor(student, item, allSubmissions, gradeBySubmission);
            boolean visible = Boolean.TRUE.equals(item.getGradesPublished());
            Gradebook.Status status = entry.status() == Gradebook.Status.GRADED && !visible
                    ? Gradebook.Status.SUBMITTED   // don't reveal that grading is done before publishing
                    : entry.status();
            Grade grade = entry.submissionId() != null ? gradeBySubmission.get(entry.submissionId()) : null;
            String fileName = allSubmissions.stream()
                    .filter(s -> s.getId().equals(entry.submissionId()) && s.getFile() != null)
                    .map(s -> s.getFile().getOriginalFilename())
                    .findFirst().orElse(null);
            result.add(new StudentGrade(item.getId(), item.getTitle(), item.getType(), item.getDeadline(),
                    status, entry.late(),
                    visible && grade != null ? grade.getPoints() : null,
                    item.getMaxPoints(),
                    visible && grade != null ? grade.getFeedback() : null,
                    entry.submissionId(), fileName));
        }
        return result;
    }

    /**
     * Finds the submission that counts for a student on an item: their own latest submission,
     * or their group's latest submission for group work. For quizzes the best attempt counts.
     */
    private Gradebook.Entry entryFor(User student, GradedContent item, List<Submission> allSubmissions,
                                     Map<Long, Grade> gradeBySubmission) {
        Comparator<Submission> newest = Comparator.comparing(Submission::getSubmittedAt).thenComparing(Submission::getId);
        Comparator<Submission> whichCounts = item instanceof Quiz
                ? Comparator.<Submission>comparingDouble(s -> {
                            Grade g = gradeBySubmission.get(s.getId());
                            return g == null ? -1 : g.getPoints();
                        }).thenComparing(newest)
                : newest;
        Optional<Submission> latest = allSubmissions.stream()
                .filter(s -> s.getContent().getId().equals(item.getId()))
                .filter(s -> s.countsFor(student.getId()))
                .max(whichCounts);
        if (latest.isEmpty()) {
            return new Gradebook.Entry(item.getId(), Gradebook.Status.NOT_SUBMITTED, false, null, null);
        }
        Submission s = latest.get();
        Grade grade = gradeBySubmission.get(s.getId());
        return grade == null
                ? new Gradebook.Entry(item.getId(), Gradebook.Status.SUBMITTED, s.isLate(), null, s.getId())
                : new Gradebook.Entry(item.getId(), Gradebook.Status.GRADED, s.isLate(), grade.getPoints(), s.getId());
    }

    private User requireTeacher(Long courseId, Long teacherId) {
        User teacher = userService.get(teacherId);
        if (!enrollments.existsByUserIdAndCourseIdAndRole(teacherId, courseId, Role.TEACHER)) {
            throw new ForbiddenException("Only teachers of course " + courseId + " can do this");
        }
        return teacher;
    }
}
