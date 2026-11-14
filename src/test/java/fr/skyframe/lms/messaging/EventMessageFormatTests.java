package fr.skyframe.lms.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import fr.skyframe.lms.domain.Role;
import fr.skyframe.lms.events.*;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Checks the JSON that goes over RabbitMQ: every event type survives the round trip through
 * the same converter the application uses. No broker needed.
 */
class EventMessageFormatTests {

    private final ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();
    private final Jackson2JsonMessageConverter converter = RabbitConfig.eventJsonConverter(objectMapper);

    @Test
    void everyEventTypeSurvivesTheRoundTrip() {
        Instant now = Instant.parse("2026-10-27T09:00:00Z");
        List<DomainEvent> events = List.of(
                new SubmissionHandedIn(UUID.randomUUID(), now, 1L, 2L, 3L, "Hand-in 1", 4L, null, true),
                new SubmissionGraded(UUID.randomUUID(), now, 1L, 2L, 3L, 8.5, 5L),
                new GradesPublished(UUID.randomUUID(), now, 2L, 3L, "Hand-in 1", true),
                new UserEnrolled(UUID.randomUUID(), now, 2L, "DAT250", 4L, Role.STUDENT));

        for (DomainEvent event : events) {
            Message message = converter.toMessage(event, new MessageProperties());
            Object back = converter.fromMessage(message);
            assertThat(back).isEqualTo(event);
        }
    }

    @Test
    void messagesArePlainJsonThatOtherComponentsCanRead() {
        SubmissionHandedIn event = new SubmissionHandedIn(UUID.randomUUID(), Instant.parse("2026-10-27T09:00:00Z"),
                1L, 2L, 3L, "Hand-in 1", 4L, null, true);

        Message message = converter.toMessage(event, new MessageProperties());
        String json = new String(message.getBody(), StandardCharsets.UTF_8);

        assertThat(message.getMessageProperties().getContentType()).isEqualTo("application/json");
        assertThat(json).contains("\"contentTitle\":\"Hand-in 1\"").contains("\"late\":true");
        assertThat(message.getMessageProperties().getHeaders().get("__TypeId__"))
                .isEqualTo(SubmissionHandedIn.class.getName());
    }

    @Test
    void classesOutsideTheEventsPackageAreNotTrusted() {
        Message message = new Message("{}".getBytes(StandardCharsets.UTF_8), new MessageProperties());
        message.getMessageProperties().setContentType("application/json");
        // A message claiming to be some other class of ours must not be turned into that class
        message.getMessageProperties().setHeader("__TypeId__", "fr.skyframe.lms.domain.User");

        assertThatThrownBy(() -> converter.fromMessage(message))
                .hasStackTraceContaining("not in the trusted packages");
    }
}
