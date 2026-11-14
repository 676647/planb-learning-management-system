package fr.skyframe.lms;

import com.fasterxml.jackson.databind.JsonNode;
import fr.skyframe.lms.support.ApiTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The WebSocket side of live quizzes, with a real server and a real STOMP client:
 * changes made through the REST API arrive as STOMP messages, and only for the right people.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class LiveWebSocketTests extends ApiTestSupport {

    @LocalServerPort
    int port;

    private WebSocketStompClient stompClient;

    @BeforeEach
    void startClient() {
        stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        converter.setObjectMapper(json);
        stompClient.setMessageConverter(converter);
    }

    @AfterEach
    void stopClient() {
        stompClient.stop();
    }

    private CompletableFuture<StompSession> connectAsync(String authorization) {
        StompHeaders headers = new StompHeaders();
        if (authorization != null) {
            headers.add("Authorization", authorization);
        }
        return stompClient.connectAsync("ws://localhost:" + port + "/api/ws", new WebSocketHttpHeaders(),
                headers, new StompSessionHandlerAdapter() { });
    }

    private StompSession connect(TestUser user) throws Exception {
        return connectAsync(user.auth()).get(5, TimeUnit.SECONDS);
    }

    /** Collects the messages of one subscription. */
    private BlockingQueue<JsonNode> subscribe(StompSession session, String destination) throws InterruptedException {
        BlockingQueue<JsonNode> messages = new LinkedBlockingQueue<>();
        session.subscribe(destination, new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return JsonNode.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                messages.add((JsonNode) payload);
            }
        });
        Thread.sleep(300); // SUBSCRIBE is asynchronous: give the server a moment to register it
        return messages;
    }

    /** Waits for the next message with the given phase (skipping e.g. "3 answered" updates). */
    private JsonNode next(BlockingQueue<JsonNode> messages, String phase) throws InterruptedException {
        long until = System.currentTimeMillis() + 5000;
        while (System.currentTimeMillis() < until) {
            JsonNode message = messages.poll(until - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
            if (message != null && phase.equals(message.path("phase").asText())) {
                return message;
            }
        }
        throw new AssertionError("No " + phase + " message within 5 seconds");
    }

    /** A course with one student and a quiz with one multiple-choice question. */
    private long[] courseAndQuiz(TestUser teacher, TestUser... students) throws Exception {
        long courseId = courseWithStudents(teacher, "DAT250", students);
        long moduleId = create(teacher, "/api/courses/" + courseId + "/modules", """
                {"title": "Week 1"}
                """);
        long quizId = create(teacher, "/api/modules/" + moduleId + "/quizzes", """
                {"title": "Friday quiz"}
                """);
        create(teacher, "/api/contents/" + quizId + "/questions/multiple-choice", """
                {"prompt": "2 + 2?", "options": ["3", "4"], "correctOptions": [1]}
                """);
        return new long[]{courseId, quizId};
    }

    private JsonNode body(org.springframework.test.web.servlet.ResultActions result) throws Exception {
        return json.readTree(result.andReturn().getResponse().getContentAsString());
    }

    @Test
    void playersReceiveTheGameAsItHappens() throws Exception {
        TestUser teacher = registerTeacher("ws-teacher1@hvl.no", "Tina Teacher");
        TestUser anna = registerAndLogin("ws-anna1@hvl.no", "Anna");
        long[] ids = courseAndQuiz(teacher, anna);

        StompSession session = connect(anna);
        BlockingQueue<JsonNode> courseNotices = subscribe(session, "/topic/courses/" + ids[0] + "/live");

        // The course page learns that a quiz started
        JsonNode game = body(postJson(teacher, "/api/contents/" + ids[1] + "/live", "{}"));
        String gameId = game.get("gameId").asText();
        JsonNode notice = courseNotices.poll(5, TimeUnit.SECONDS);
        assertThat(notice).isNotNull();
        assertThat(notice.get("active").asBoolean()).isTrue();
        assertThat(notice.get("pin").asText()).isEqualTo(game.get("pin").asText());

        BlockingQueue<JsonNode> updates = subscribe(session, "/topic/live/" + gameId);
        BlockingQueue<JsonNode> mine = subscribe(session, "/user/queue/live");

        postJson(anna, "/api/live/join", """
                {"pin": "%s"}
                """.formatted(game.get("pin").asText()));
        assertThat(next(updates, "LOBBY").get("players").get(0).asText()).isEqualTo("Anna");

        postJson(teacher, "/api/live/" + gameId + "/next", "{}");
        JsonNode question = next(updates, "QUESTION");
        assertThat(question.get("question").get("prompt").asText()).isEqualTo("2 + 2?");
        assertThat(question.has("reveal")).isFalse();

        // The only player answers, so the answer is revealed at once, to everyone and personally
        postJson(anna, "/api/live/" + gameId + "/answers", """
                {"questionNumber": 1, "answer": ["1"]}
                """);
        JsonNode reveal = next(updates, "REVEAL");
        assertThat(reveal.get("reveal").get("correctAnswer").get(0).asText()).isEqualTo("1");
        JsonNode personal = mine.poll(5, TimeUnit.SECONDS);
        assertThat(personal).isNotNull();
        assertThat(personal.get("gameId").asText()).isEqualTo(gameId);
        assertThat(personal.get("me").get("lastAnswer").get("correct").asBoolean()).isTrue();
        assertThat(personal.get("me").get("score").asInt()).isGreaterThan(900); // answered within a second

        postJson(teacher, "/api/live/" + gameId + "/next", "{}");
        assertThat(next(updates, "FINISHED").get("leaderboard").get(0).get("name").asText()).isEqualTo("Anna");
        JsonNode ended = courseNotices.poll(5, TimeUnit.SECONDS);
        assertThat(ended).isNotNull();
        assertThat(ended.get("active").asBoolean()).isFalse();
        session.disconnect();
    }

    @Test
    void connectingNeedsAValidToken() {
        assertThatThrownBy(() -> connectAsync(null).get(5, TimeUnit.SECONDS))
                .isInstanceOfAny(ExecutionException.class, TimeoutException.class);
        assertThatThrownBy(() -> connectAsync("Bearer not-a-token").get(5, TimeUnit.SECONDS))
                .isInstanceOfAny(ExecutionException.class, TimeoutException.class);
    }

    @Test
    void outsidersCannotListenToAGame() throws Exception {
        TestUser teacher = registerTeacher("ws-teacher3@hvl.no", "Tina Teacher");
        TestUser anna = registerAndLogin("ws-anna3@hvl.no", "Anna");
        TestUser outsider = registerAndLogin("ws-outsider3@hvl.no", "Olga Outsider");
        long[] ids = courseAndQuiz(teacher, anna);
        String gameId = body(postJson(teacher, "/api/contents/" + ids[1] + "/live", "{}")).get("gameId").asText();

        StompSession session = connect(outsider);
        BlockingQueue<JsonNode> updates = subscribe(session, "/topic/live/" + gameId);

        // The server refuses the subscription with an ERROR frame and closes the connection
        long until = System.currentTimeMillis() + 5000;
        while (session.isConnected() && System.currentTimeMillis() < until) {
            Thread.sleep(50);
        }
        assertThat(session.isConnected()).isFalse();
        postJson(teacher, "/api/live/" + gameId + "/end", "{}");
        assertThat(updates.poll(500, TimeUnit.MILLISECONDS)).isNull();
    }
}
