package com.cnh.ies.service.notification;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Routes Redis pub/sub messages to active SSE connections.
 * Uses a single pattern subscription so reconnects only update in-memory state.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationSseRegistry {

    private static final PatternTopic NOTIFICATION_TOPIC =
            new PatternTopic(NotificationService.NOTIFICATION_CHANNEL_PREFIX + "*");

    private final RedisMessageListenerContainer redisMessageListenerContainer;
    private final Map<UUID, SseEmitter> emitters = new ConcurrentHashMap<>();

    @PostConstruct
    void registerRedisListener() {
        redisMessageListenerContainer.addMessageListener(this::onRedisMessage, NOTIFICATION_TOPIC);
        log.info("Registered Redis pattern listener on {}", NOTIFICATION_TOPIC.getTopic());
    }

    public SseEmitter register(UUID userId, long timeoutMs) {
        close(userId);

        SseEmitter emitter = new SseEmitter(timeoutMs);
        emitters.put(userId, emitter);

        emitter.onCompletion(() -> removeIfCurrent(userId, emitter));
        emitter.onTimeout(() -> removeIfCurrent(userId, emitter));
        emitter.onError(e -> removeIfCurrent(userId, emitter));

        return emitter;
    }

    public void close(UUID userId) {
        SseEmitter existing = emitters.remove(userId);
        if (existing != null) {
            try {
                existing.complete();
            } catch (IllegalStateException ignored) {
                // already completed
            }
        }
    }

    private void onRedisMessage(Message message, byte[] pattern) {
        UUID userId = parseUserId(message);
        if (userId == null) {
            return;
        }

        SseEmitter emitter = emitters.get(userId);
        if (emitter == null) {
            return;
        }

        try {
            emitter.send(SseEmitter.event()
                    .name("notification")
                    .data(new String(message.getBody())));
        } catch (IOException | IllegalStateException e) {
            log.debug("SSE client disconnected for user {}, skip push: {}", userId, e.getMessage());
            removeIfCurrent(userId, emitter);
        }
    }

    private UUID parseUserId(Message message) {
        String channel = new String(message.getChannel());
        String prefix = NotificationService.NOTIFICATION_CHANNEL_PREFIX;
        if (!channel.startsWith(prefix)) {
            return null;
        }
        try {
            return UUID.fromString(channel.substring(prefix.length()));
        } catch (IllegalArgumentException e) {
            log.warn("Invalid notification channel: {}", channel);
            return null;
        }
    }

    private void removeIfCurrent(UUID userId, SseEmitter emitter) {
        emitters.remove(userId, emitter);
    }
}
