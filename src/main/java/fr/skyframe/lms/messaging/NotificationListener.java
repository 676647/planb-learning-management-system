package fr.skyframe.lms.messaging;

import fr.skyframe.lms.events.DomainEvent;
import fr.skyframe.lms.service.NotificationService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * A consumer of the domain events: receives every event from the "lms.notifications" queue
 * and turns it into in-app notifications. It knows nothing about who sent the event.
 */
@Component
@ConditionalOnProperty(name = "lms.messaging.type", havingValue = "rabbit")
public class NotificationListener {

    private final NotificationService notificationService;

    public NotificationListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @RabbitListener(queues = RabbitConfig.NOTIFICATION_QUEUE)
    public void onEvent(DomainEvent event) {
        notificationService.handle(event);
    }
}
