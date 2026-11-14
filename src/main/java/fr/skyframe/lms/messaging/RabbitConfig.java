package fr.skyframe.lms.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.DefaultJackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ setup (only when lms.messaging.type=rabbit):
 *
 *   services ──► exchange "lms.events" (topic) ──"#"──► queue "lms.notifications" ──► NotificationListener
 *                                                                  │ fails 3×
 *                                                                  ▼
 *                                                     queue "lms.notifications.dlq"
 *
 * A topic exchange routes by routing key, e.g. "submission.handed-in". Our notification queue
 * takes every event ("#"); another component could bind its own queue to just "submission.*".
 * Spring creates the exchange, queues and binding in RabbitMQ on startup.
 */
@Configuration
@ConditionalOnProperty(name = "lms.messaging.type", havingValue = "rabbit")
public class RabbitConfig {

    public static final String EXCHANGE = "lms.events";
    public static final String NOTIFICATION_QUEUE = "lms.notifications";
    public static final String NOTIFICATION_DLQ = "lms.notifications.dlq";

    @Bean
    TopicExchange eventsExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    Queue notificationQueue() {
        // Messages that fail repeatedly are moved to the dead-letter queue instead of being lost or retried forever
        return QueueBuilder.durable(NOTIFICATION_QUEUE)
                .deadLetterExchange("")
                .deadLetterRoutingKey(NOTIFICATION_DLQ)
                .build();
    }

    @Bean
    Queue notificationDeadLetterQueue() {
        return QueueBuilder.durable(NOTIFICATION_DLQ).build();
    }

    @Bean
    Binding notificationBinding(Queue notificationQueue, TopicExchange eventsExchange) {
        return BindingBuilder.bind(notificationQueue).to(eventsExchange).with("#");
    }

    /** Messages are sent as JSON. Spring Boot uses this converter for sending and for @RabbitListener. */
    @Bean
    MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        return eventJsonConverter(objectMapper);
    }

    /**
     * The converter adds a "__TypeId__" header with the event's class name, so the receiver knows
     * which record to create. For safety it only creates classes from our events package.
     */
    public static Jackson2JsonMessageConverter eventJsonConverter(ObjectMapper objectMapper) {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter(objectMapper);
        DefaultJackson2JavaTypeMapper typeMapper = new DefaultJackson2JavaTypeMapper();
        typeMapper.setTrustedPackages("fr.skyframe.lms.events");
        converter.setJavaTypeMapper(typeMapper);
        return converter;
    }
}
