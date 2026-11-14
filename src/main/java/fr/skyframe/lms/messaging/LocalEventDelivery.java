package fr.skyframe.lms.messaging;

import fr.skyframe.lms.events.DomainEvent;
import fr.skyframe.lms.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Used instead of RabbitMQ when lms.messaging.type=local (the tests, or running without RabbitMQ):
 * delivers each event straight to the notification service after the transaction commits.
 * The notification logic is the same; only the transport differs.
 */
@Component
@ConditionalOnProperty(name = "lms.messaging.type", havingValue = "local")
public class LocalEventDelivery {

    private static final Logger log = LoggerFactory.getLogger(LocalEventDelivery.class);

    private final NotificationService notificationService;

    public LocalEventDelivery(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void deliver(DomainEvent event) {
        try {
            notificationService.handle(event);
        } catch (RuntimeException e) {
            // The user's change is already committed; a failing listener must not turn it into an error
            log.error("Handling event {} failed", event, e);
        }
    }
}
