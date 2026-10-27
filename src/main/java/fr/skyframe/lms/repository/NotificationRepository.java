package fr.skyframe.lms.repository;

import fr.skyframe.lms.domain.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /** The 50 newest notifications of a user. */
    List<Notification> findTop50ByRecipientIdOrderByCreatedAtDescIdDesc(Long recipientId);

    List<Notification> findByRecipientIdAndReadFalse(Long recipientId);

    long countByRecipientIdAndReadFalse(Long recipientId);

    Optional<Notification> findByIdAndRecipientId(Long id, Long recipientId);

    boolean existsByEventIdAndRecipientId(UUID eventId, Long recipientId);
}
