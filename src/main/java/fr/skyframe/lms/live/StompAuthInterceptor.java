package fr.skyframe.lms.live;

import fr.skyframe.lms.repository.EnrollmentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Security for the WebSocket. Browsers can't send an Authorization header when opening a
 * WebSocket, so the handshake itself is open (SecurityConfig) and the check happens here,
 * on the STOMP frames:
 *
 * - CONNECT must carry "Authorization: Bearer <jwt>", the same token as for the REST API.
 *   The user becomes the session's user, so /user/queue/... reaches the right person.
 * - SUBSCRIBE is only allowed to a game the user hosts, plays or whose course they are in,
 *   to their course's live notices, and to their own /user/queue/live.
 * - SEND is refused: commands go through the REST API, which has all the normal checks.
 *
 * Throwing here makes Spring answer with a STOMP ERROR frame and close the connection.
 */
@Component
public class StompAuthInterceptor implements ChannelInterceptor {

    private static final Logger log = LoggerFactory.getLogger(StompAuthInterceptor.class);

    private static final Pattern GAME_TOPIC = Pattern.compile("^/topic/live/([\\w-]+)$");
    private static final Pattern COURSE_TOPIC = Pattern.compile("^/topic/courses/(\\d+)/live$");

    private final JwtDecoder jwtDecoder;
    private final EnrollmentRepository enrollments;
    /** Looked up when needed: LiveGameService needs the messaging template that this configuration creates. */
    private final ObjectProvider<LiveGameService> liveGames;

    public StompAuthInterceptor(JwtDecoder jwtDecoder, EnrollmentRepository enrollments,
                                ObjectProvider<LiveGameService> liveGames) {
        this.jwtDecoder = jwtDecoder;
        this.enrollments = enrollments;
        this.liveGames = liveGames;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) {
            return message; // e.g. heartbeats
        }
        switch (accessor.getCommand()) {
            case CONNECT -> {
                JwtAuthenticationToken user = authenticate(accessor.getFirstNativeHeader("Authorization"));
                accessor.setUser(user);
                log.info("Live connection opened by {}", user.getName());
            }
            case SUBSCRIBE -> checkSubscription(accessor.getUser(), accessor.getDestination());
            case SEND -> throw refuse("Send commands through the REST API");
            default -> { }
        }
        return message;
    }

    /** Logged, so a refused connection shows up in the backend's log (the browser only sees "closed"). */
    private static MessagingException refuse(String reason) {
        log.warn("Live connection refused: {}", reason);
        return new MessagingException(reason);
    }

    private JwtAuthenticationToken authenticate(String header) {
        if (header == null || !header.startsWith("Bearer ")) {
            throw refuse("Log in first: CONNECT needs an Authorization: Bearer header");
        }
        try {
            Jwt jwt = jwtDecoder.decode(header.substring("Bearer ".length()));
            // The name is the subject (the e-mail), as in the REST API
            return new JwtAuthenticationToken(jwt, List.of(), jwt.getSubject());
        } catch (JwtException e) {
            throw refuse("Invalid or expired token");
        }
    }

    private void checkSubscription(Principal principal, String destination) {
        if (!(principal instanceof JwtAuthenticationToken token)) {
            throw refuse("Not logged in");
        }
        Long userId = ((Number) token.getToken().getClaim("uid")).longValue();
        if (destination == null) {
            throw refuse("No destination");
        }
        if (destination.equals("/user/queue/live")) {
            return;
        }
        Matcher game = GAME_TOPIC.matcher(destination);
        if (game.matches() && liveGames.getObject().canWatch(game.group(1), userId)) {
            return;
        }
        Matcher course = COURSE_TOPIC.matcher(destination);
        if (course.matches() && enrollments.existsByUserIdAndCourseId(userId, Long.parseLong(course.group(1)))) {
            return;
        }
        throw refuse("You may not subscribe to " + destination);
    }
}
