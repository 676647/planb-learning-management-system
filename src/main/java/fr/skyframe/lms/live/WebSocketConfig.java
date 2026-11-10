package fr.skyframe.lms.live;

import org.springframework.beans.factory.annotation.Value;
import jakarta.annotation.PreDestroy;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.List;

/**
 * WebSockets with STOMP for live quizzes.
 *
 * Browsers open one WebSocket to /api/ws (under /api, so it goes through the same nginx
 * and Vite proxies as the REST API; not under /api/live/, where it would clash with
 * GET /api/live/{gameId}) and speak STOMP over it: they SUBSCRIBE to destinations and
 * the server pushes messages to them. The simple in-memory broker in Spring keeps track of
 * the subscriptions. Who may connect and subscribe is checked in StompAuthInterceptor.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final StompAuthInterceptor authInterceptor;
    private final List<String> allowedOrigins;
    /**
     * Sends the heartbeats. Not a bean on purpose: Spring's WebSocket setup already defines a
     * TaskScheduler bean, and a second one would make injecting "the" TaskScheduler ambiguous.
     */
    private final ThreadPoolTaskScheduler heartbeatScheduler = new ThreadPoolTaskScheduler();

    public WebSocketConfig(StompAuthInterceptor authInterceptor,
                           @Value("${lms.cors.allowed-origins}") List<String> allowedOrigins) {
        this.authInterceptor = authInterceptor;
        this.allowedOrigins = allowedOrigins;
        heartbeatScheduler.setPoolSize(1);
        heartbeatScheduler.setThreadNamePrefix("ws-heartbeat-");
        heartbeatScheduler.initialize();
    }

    @PreDestroy
    void shutdown() {
        heartbeatScheduler.shutdown();
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Same-origin connections (through nginx or the Vite proxy) are always allowed;
        // the CORS origins are allowed as well, e.g. for a frontend on another port.
        registry.addEndpoint("/api/ws").setAllowedOriginPatterns(allowedOrigins.toArray(String[]::new));
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue")
                // Heartbeats every 10 s keep idle connections (e.g. in the lobby) from being closed by proxies
                .setHeartbeatValue(new long[]{10_000, 10_000})
                .setTaskScheduler(heartbeatScheduler);
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(authInterceptor);
    }
}
