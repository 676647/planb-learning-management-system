package fr.skyframe.lms.live;

import fr.skyframe.lms.live.dto.CourseLiveNotice;
import fr.skyframe.lms.live.dto.LiveGameView;
import fr.skyframe.lms.live.dto.LivePersonalUpdate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * Pushes live quiz updates to the browsers over WebSocket/STOMP. The destinations:
 *
 * - /topic/live/{gameId}           the game for everyone in it (host and players)
 * - /user/queue/live               one player's own score and feedback (Spring routes it to that user's sessions)
 * - /topic/courses/{courseId}/live a live quiz started or ended in the course (for the Join button)
 */
@Component
public class LiveGameMessenger {

    private final SimpMessagingTemplate template;

    public LiveGameMessenger(SimpMessagingTemplate template) {
        this.template = template;
    }

    public void gameChanged(LiveGameView view) {
        template.convertAndSend("/topic/live/" + view.gameId(), view);
    }

    /** @param email the user's e-mail, which is the name of their WebSocket session's user (the JWT subject) */
    public void toPlayer(String email, LivePersonalUpdate update) {
        template.convertAndSendToUser(email, "/queue/live", update);
    }

    public void courseChanged(Long courseId, CourseLiveNotice notice) {
        template.convertAndSend("/topic/courses/" + courseId + "/live", notice);
    }
}
