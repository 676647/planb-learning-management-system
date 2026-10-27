package fr.skyframe.lms;

import fr.skyframe.lms.storage.FileStorage;
import fr.skyframe.lms.storage.InMemoryFileStorage;
import fr.skyframe.lms.support.ApiTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.ResultActions;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Uploading and downloading files with submissions. Like all tests, this runs with the
 * in-memory file store instead of MongoDB (see src/test/resources/config/application.properties),
 * so no MongoDB server is needed.
 */
@SpringBootTest
@AutoConfigureMockMvc
class FileSubmissionApiTests extends ApiTestSupport {

    static final byte[] PDF = "%PDF-1.7 pretend this is a report".getBytes(StandardCharsets.UTF_8);

    @Autowired
    FileStorage storage;

    @Test
    void studentUploadsAndTeacherDownloads() throws Exception {
        Setup s = setUp("1");

        String body = upload(s.anna, s.assignmentId, new MockMultipartFile("file", "Report Anna.pdf", "application/pdf", PDF), "See attached")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.answer").value("See attached"))
                .andExpect(jsonPath("$.file.name").value("Report Anna.pdf"))
                .andExpect(jsonPath("$.file.size").value(PDF.length))
                .andExpect(jsonPath("$.file.storageKey").doesNotExist())   // internal detail stays internal
                .andReturn().getResponse().getContentAsString();
        long submissionId = json.readTree(body).get("id").asLong();

        // The teacher downloads exactly what was uploaded, as an attachment
        byte[] downloaded = getAs(s.teacher, "/api/submissions/" + submissionId + "/file")
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, org.hamcrest.Matchers.startsWith("attachment")))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, org.hamcrest.Matchers.containsString("Report")))
                .andReturn().getResponse().getContentAsByteArray();
        assertThat(downloaded).isEqualTo(PDF);

        // So can the student who handed it in, but not another student
        getAs(s.anna, "/api/submissions/" + submissionId + "/file").andExpect(status().isOk());
        getAs(s.bob, "/api/submissions/" + submissionId + "/file").andExpect(status().isForbidden());

        // The student's overview shows the file name
        getAs(s.anna, "/api/courses/" + s.courseId + "/my-grades")
                .andExpect(jsonPath("$[0].fileName").value("Report Anna.pdf"))
                .andExpect(jsonPath("$[0].submissionId").value(submissionId));
    }

    @Test
    void fileOnlySubmissionIsAccepted() throws Exception {
        Setup s = setUp("2");
        upload(s.anna, s.assignmentId, new MockMultipartFile("file", "report.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document", PDF), null)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.file.name").value("report.docx"));
    }

    @Test
    void disallowedFileTypeIsRejected() throws Exception {
        Setup s = setUp("3");
        upload(s.anna, s.assignmentId, new MockMultipartFile("file", "virus.exe", "application/octet-stream", PDF), null)
                .andExpect(status().isBadRequest());
        assertThat(((InMemoryFileStorage) storage).countWithPrefix("submissions/" + s.assignmentId + "/")).isZero();
    }

    @Test
    void emptySubmissionIsRejected() throws Exception {
        Setup s = setUp("4");
        mvc.perform(multipart("/api/contents/{id}/submissions", s.assignmentId)
                        .header(HttpHeaders.AUTHORIZATION, s.anna.auth()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectedSubmissionDoesNotStoreTheFile() throws Exception {
        Setup s = setUp("5");
        // Teachers can't hand in, so the file must not end up in storage either
        upload(s.teacher, s.assignmentId, new MockMultipartFile("file", "report.pdf", "application/pdf", PDF), null)
                .andExpect(status().isForbidden());
        assertThat(((InMemoryFileStorage) storage).countWithPrefix("submissions/" + s.assignmentId + "/")).isZero();
    }

    @Test
    void deletingTheAssignmentDeletesItsFiles() throws Exception {
        Setup s = setUp("6");
        upload(s.anna, s.assignmentId, new MockMultipartFile("file", "report.pdf", "application/pdf", PDF), null)
                .andExpect(status().isCreated());
        String prefix = "submissions/" + s.assignmentId + "/";
        assertThat(((InMemoryFileStorage) storage).countWithPrefix(prefix)).isEqualTo(1);

        deleteAs(s.teacher, "/api/contents/" + s.assignmentId).andExpect(status().isNoContent());

        assertThat(((InMemoryFileStorage) storage).countWithPrefix(prefix)).isZero();
    }

    // --- helpers ---

    record Setup(TestUser teacher, TestUser anna, TestUser bob, long courseId, long assignmentId) {
    }

    /** A course with two students and one assignment. n keeps e-mails unique per test. */
    private Setup setUp(String n) throws Exception {
        TestUser teacher = registerAndLogin("file-teacher" + n + "@hvl.no", "Tina Teacher");
        TestUser anna = registerAndLogin("file-anna" + n + "@hvl.no", "Anna");
        TestUser bob = registerAndLogin("file-bob" + n + "@hvl.no", "Bob");
        long courseId = courseWithStudents(teacher, "DAT250", anna, bob);
        long moduleId = create(teacher, "/api/courses/" + courseId + "/modules", """
                {"title": "Week 1"}
                """);
        long assignmentId = create(teacher, "/api/modules/" + moduleId + "/assignments", """
                {"title": "Report", "deadline": "2099-01-01T00:00:00Z", "maxPoints": 10}
                """);
        return new Setup(teacher, anna, bob, courseId, assignmentId);
    }

    private ResultActions upload(TestUser user, long contentId, MockMultipartFile file, String answer) throws Exception {
        var request = multipart("/api/contents/{id}/submissions", contentId)
                .file(file)
                .header(HttpHeaders.AUTHORIZATION, user.auth());
        if (answer != null) {
            request.param("answer", answer);
        }
        return mvc.perform(request);
    }
}
