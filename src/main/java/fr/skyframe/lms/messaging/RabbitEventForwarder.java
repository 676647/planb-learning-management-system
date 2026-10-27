package fr.skyframe.lms.messaging;

import fr.skyframe.lms.events.DomainEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Sends the services' domain events to RabbitMQ.
 *
 * The services publish events inside their database transaction. This listener runs only
 * AFTER_COMMIT, so nothing is announced for work that was rolled back (e.g. a hand-in that
 * failed to save).
 *
 * Known limitation: if RabbitMQ is down at that moment, the event is logged and lost (the
 * database change itself is kept). The "transactional outbox" pattern would fix this by first
 * saving events in the database and sending them from there.
 */
@Component
@ConditionalOnProperty(name = "lms.messaging.type", havingValue = "rabbit")
public class RabbitEventForwarder {

    private static final Logger log = LoggerFactory.getLogger(RabbitEventForwarder.class);

    private final RabbitTemplate rabbit;

    public RabbitEventForwarder(RabbitTemplate rabbit) {
        this.rabbit = rabbit;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void forward(DomainEvent event) {
        try {
            rabbit.convertAndSend(RabbitConfig.EXCHANGE, event.routingKey(), event);
            log.info("Sent {} ({}) to RabbitMQ", event.routingKey(), event.eventId());
        } catch (AmqpException e) {
            // Don't fail the user's request: their change is already saved
            log.error("Could not send event {} to RabbitMQ; it is lost: {}", event, e.getMessage());
        }
    }
}
