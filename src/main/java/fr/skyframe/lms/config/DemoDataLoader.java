package fr.skyframe.lms.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.skyframe.lms.domain.AccountRole;
import fr.skyframe.lms.domain.Content;
import fr.skyframe.lms.domain.Course;
import fr.skyframe.lms.domain.CourseModule;
import fr.skyframe.lms.domain.Question;
import fr.skyframe.lms.domain.Role;
import fr.skyframe.lms.domain.User;
import fr.skyframe.lms.repository.UserRepository;
import fr.skyframe.lms.service.ContentService;
import fr.skyframe.lms.service.CourseService;
import fr.skyframe.lms.service.ModuleService;
import fr.skyframe.lms.service.QuizService;
import fr.skyframe.lms.service.UserService;
import fr.skyframe.lms.web.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

/**
 * Inserts test data on startup: the users, courses, modules, pages, assignments and quizzes
 * in src/main/resources/demo-data.json. Handy for development and demos.
 *
 * - Turned on with lms.demo-data.enabled=true (DEMO_DATA=true); off in the tests.
 * - Inserted only once: if the file's first user already exists, nothing happens.
 * - All or nothing: it runs in one transaction, so an error in the file leaves the database
 *   as it was (and is shown in the log).
 * - Goes through the normal services, so the same rules apply as for the REST API.
 */
@Component
@ConditionalOnProperty(name = "lms.demo-data.enabled", havingValue = "true")
public class DemoDataLoader implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataLoader.class);

    /** "type" of an item in the file → the request record it is read as (the same as the REST API's). */
    private static final Map<String, Class<?>> ITEM_TYPES = Map.of(
            "page", PageRequest.class,
            "assignment", AssignmentRequest.class,
            "quiz", QuizRequest.class,
            "coding-exercise", CodingExerciseRequest.class);

    /** "kind" of a question in the file → its request record. */
    private static final Map<String, Class<?>> QUESTION_KINDS = Map.of(
            "multiple-choice", MultipleChoiceQuestionRequest.class,
            "ordering", OrderingQuestionRequest.class,
            "matching", MatchingQuestionRequest.class,
            "fill-in-blanks", FillInBlanksQuestionRequest.class);

    private final ObjectMapper json;
    private final Resource file;
    private final UserService userService;
    private final UserRepository users;
    private final CourseService courseService;
    private final ModuleService moduleService;
    private final ContentService contentService;
    private final QuizService quizService;
    private final Clock clock;

    public DemoDataLoader(ObjectMapper json, @Value("${lms.demo-data.file:classpath:demo-data.json}") Resource file,
                          UserService userService, UserRepository users, CourseService courseService,
                          ModuleService moduleService, ContentService contentService, QuizService quizService,
                          Clock clock) {
        this.json = json;
        this.file = file;
        this.userService = userService;
        this.users = users;
        this.courseService = courseService;
        this.moduleService = moduleService;
        this.contentService = contentService;
        this.quizService = quizService;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws IOException {
        JsonNode data;
        try (InputStream in = file.getInputStream()) {
            data = json.readTree(in);
        }
        String firstEmail = data.path("users").path(0).path("email").asText();
        if (users.existsByEmail(firstEmail.toLowerCase())) {
            log.info("Test data is already in the database ({} exists); nothing inserted", firstEmail);
            return;
        }

        String password = data.path("password").asText();
        for (JsonNode u : data.path("users")) {
            User user = userService.register(u.path("email").asText(), u.path("name").asText(), password);
            if (u.has("accountRole")) {
                user.setAccountRole(AccountRole.valueOf(u.path("accountRole").asText()));
            }
        }
        int courses = 0;
        for (JsonNode c : data.path("courses")) {
            addCourse(c);
            courses++;
        }
        log.info("Inserted test data: {} users and {} courses. Log in with e.g. {} / {}",
                data.path("users").size(), courses, firstEmail, password);
    }

    private void addCourse(JsonNode c) throws IOException {
        User teacher = userService.getByEmail(c.path("teacher").asText());
        Course course = courseService.create(teacher.getId(), c.path("title").asText(),
                c.path("description").asText(null), c.path("semester").asText(null));
        for (JsonNode email : c.path("coTeachers")) {
            courseService.enroll(course.getId(), email.asText(), Role.TEACHER);
        }
        for (JsonNode email : c.path("students")) {
            courseService.enroll(course.getId(), email.asText(), Role.STUDENT);
        }
        for (JsonNode m : c.path("modules")) {
            CourseModule module = moduleService.create(course.getId(), m.path("title").asText());
            for (JsonNode item : m.path("items")) {
                addItem(module, item);
            }
        }
    }

    private void addItem(CourseModule module, JsonNode item) throws IOException {
        String type = item.path("type").asText();
        Class<?> requestType = ITEM_TYPES.get(type);
        if (requestType == null) {
            throw new IllegalArgumentException("Unknown item type '" + type + "' in demo-data.json; use one of "
                    + ITEM_TYPES.keySet());
        }
        Content content = contentService.add(module.getId(), toEntity(withDates(item), requestType, Content.class));
        for (JsonNode q : item.path("questions")) {
            String kind = q.path("kind").asText();
            Class<?> questionType = QUESTION_KINDS.get(kind);
            if (questionType == null) {
                throw new IllegalArgumentException("Unknown question kind '" + kind + "' in demo-data.json; use one of "
                        + QUESTION_KINDS.keySet());
            }
            ObjectNode question = q.deepCopy();
            question.remove("kind");
            quizService.addQuestion(content.getId(), toEntity(question, questionType, Question.class));
        }
    }

    /**
     * Reads the JSON as the API's request record and calls its toEntity(), so the file uses
     * exactly the same fields as the REST API.
     */
    private <T> T toEntity(JsonNode node, Class<?> requestType, Class<T> entityType) throws IOException {
        Object request = json.treeToValue(node, requestType);
        try {
            return entityType.cast(requestType.getMethod("toEntity").invoke(request));
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(requestType.getSimpleName() + ".toEntity() failed", e);
        }
    }

    /** Turns "deadlineInDays": 7 etc. into real dates, counted from today. */
    private JsonNode withDates(JsonNode item) {
        ObjectNode copy = item.deepCopy();
        Instant today = Instant.now(clock).truncatedTo(ChronoUnit.HOURS);
        for (String field : new String[]{"deadline", "visibleFrom", "visibleUntil"}) {
            String relative = field + "InDays";
            if (copy.has(relative)) {
                copy.put(field, today.plus(copy.path(relative).asLong(), ChronoUnit.DAYS).toString());
                copy.remove(relative);
            }
        }
        copy.remove("type");
        copy.remove("questions");
        return copy;
    }
}
