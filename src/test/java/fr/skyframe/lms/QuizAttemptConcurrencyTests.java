package fr.skyframe.lms;

import fr.skyframe.lms.domain.*;
import fr.skyframe.lms.repository.SubmissionRepository;
import fr.skyframe.lms.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Quiz attempts sent at the same moment must not get past the attempt limit.
 * Not @Transactional: every attempt runs in its own transaction on its own thread, as real requests do.
 * The data is committed, so the e-mail addresses are unique per run.
 */
@SpringBootTest
class QuizAttemptConcurrencyTests {

    @Autowired UserService userService;
    @Autowired CourseService courseService;
    @Autowired ModuleService moduleService;
    @Autowired ContentService contentService;
    @Autowired QuizService quizService;
    @Autowired SubmissionRepository submissions;

    @Test
    void simultaneousAttemptsCannotExceedTheLimit() throws Exception {
        String tag = UUID.randomUUID().toString().substring(0, 8);
        User teacher = userService.register("race-teacher-" + tag + "@hvl.no", "Tina Teacher", "secret-password");
        User student = userService.register("race-student-" + tag + "@hvl.no", "Sam Student", "secret-password");
        Course course = courseService.create(teacher.getId(), "Race " + tag, null, "Autumn 2026");
        courseService.enroll(course.getId(), student.getEmail(), Role.STUDENT);
        CourseModule module = moduleService.create(course.getId(), "Week 1");

        Quiz quiz = new Quiz();
        quiz.setTitle("One try only");
        quiz.setMaxAttempts(1);
        Long quizId = contentService.add(module.getId(), quiz).getId();
        MultipleChoiceQuestion question = new MultipleChoiceQuestion();
        question.setPrompt("Which is a relational database?");
        question.setOptions(List.of("PostgreSQL", "MongoDB"));
        question.setCorrectOptions(Set.of(0));
        Long questionId = quizService.addQuestion(quizId, question).getId();
        Map<Long, List<String>> answers = Map.of(questionId, List.of("0"));

        int requests = 5;
        ExecutorService pool = Executors.newFixedThreadPool(requests);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Boolean>> results = new ArrayList<>();
        for (int i = 0; i < requests; i++) {
            results.add(pool.submit(() -> {
                start.await();
                try {
                    quizService.attempt(quizId, student.getId(), answers);
                    return true;
                } catch (ConflictException noAttemptsLeft) {
                    return false;
                }
            }));
        }
        start.countDown(); // all threads go at once

        int accepted = 0;
        try {
            for (Future<Boolean> result : results) {
                if (result.get(30, TimeUnit.SECONDS)) {
                    accepted++;
                }
            }
        } finally {
            pool.shutdownNow();
        }

        assertThat(accepted).isEqualTo(1);
        assertThat(submissions.countByContentIdAndSubmittedById(quizId, student.getId())).isEqualTo(1);
    }
}
