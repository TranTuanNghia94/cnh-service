package com.cnh.ies.controller;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.cnh.ies.dto.common.ApiResponse;
import com.cnh.ies.model.notification.CreateNotificationRequest;
import com.cnh.ies.model.notification.NotificationInfo;
import com.cnh.ies.model.notification.NotificationSummary;
import com.cnh.ies.service.notification.NotificationService;
import com.cnh.ies.service.security.AuthenticationUserDetails;
import com.cnh.ies.util.RequestContext;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
@Slf4j
public class NotificationController {

    private static final long SSE_TIMEOUT = 30 * 60 * 1000L; // 30 minutes

    private final NotificationService notificationService;
    private final RedisMessageListenerContainer redisMessageListenerContainer;
    
    private final Map<UUID, SseEmitter> userEmitters = new ConcurrentHashMap<>();

    @GetMapping
    public ApiResponse<NotificationSummary> getNotifications(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int limit) {
        UUID userId = getCurrentUserId(userDetails);
        NotificationSummary response = notificationService.getNotifications(userId, page, limit, RequestContext.getRequestId());
        return ApiResponse.success(response, "Get notifications success");
    }

    @GetMapping("/unread")
    public ApiResponse<List<NotificationInfo>> getUnreadNotifications(
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = getCurrentUserId(userDetails);
        List<NotificationInfo> response = notificationService.getUnreadNotifications(userId, RequestContext.getRequestId());
        return ApiResponse.success(response, "Get unread notifications success");
    }

    @GetMapping("/unread-count")
    public ApiResponse<Map<String, Long>> getUnreadCount(
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = getCurrentUserId(userDetails);
        long count = notificationService.getUnreadCount(userId, RequestContext.getRequestId());
        return ApiResponse.success(Map.of("count", count), "Get unread count success");
    }

    /** PUT is REST-typical; POST is accepted for clients that use POST for state-changing actions. */
    @RequestMapping(value = "/{id}/read", method = {RequestMethod.PUT, RequestMethod.POST})
    public ApiResponse<String> markAsRead(
            @PathVariable String id,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = getCurrentUserId(userDetails);
        notificationService.markAsRead(UUID.fromString(id), userId, RequestContext.getRequestId());
        return ApiResponse.success("OK", "Notification marked as read");
    }

    @PutMapping("/read-all")
    public ApiResponse<Map<String, Integer>> markAllAsRead(
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = getCurrentUserId(userDetails);
        int count = notificationService.markAllAsRead(userId, RequestContext.getRequestId());
        return ApiResponse.success(Map.of("markedCount", count), "All notifications marked as read");
    }

    @PostMapping
    public ApiResponse<List<NotificationInfo>> createNotifications(
            @RequestBody CreateNotificationRequest request) {
        List<NotificationInfo> response = notificationService.createNotifications(request, RequestContext.getRequestId());
        return ApiResponse.success(response, "Notifications created successfully");
    }

    @DeleteMapping("/{id}")
    public ApiResponse<String> deleteNotification(
            @PathVariable String id,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = getCurrentUserId(userDetails);
        notificationService.deleteNotification(UUID.fromString(id), userId, RequestContext.getRequestId());
        return ApiResponse.success("OK", "Notification deleted");
    }

    /**
     * SSE endpoint for real-time notifications.
     * Frontend can subscribe to this endpoint to receive notifications in real-time.
     * 
     * Usage: EventSource('/api/v1/notifications/subscribe')
     */
    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = getCurrentUserId(userDetails);
        log.info("User {} subscribing to notifications", userId);

        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);
        
        // Remove old emitter if exists
        SseEmitter oldEmitter = userEmitters.put(userId, emitter);
        if (oldEmitter != null) {
            oldEmitter.complete();
        }

        // Subscribe to Redis channel for this user
        String channel = notificationService.getNotificationChannel(userId);
        MessageListener listener = (message, pattern) -> {
            try {
                String notification = new String(message.getBody());
                emitter.send(SseEmitter.event()
                        .name("notification")
                        .data(notification));
            } catch (IOException e) {
                log.warn("Error sending SSE notification to user {}: {}", userId, e.getMessage());
                emitter.completeWithError(e);
            }
        };

        redisMessageListenerContainer.addMessageListener(listener, new ChannelTopic(channel));

        emitter.onCompletion(() -> {
            log.info("SSE connection completed for user {}", userId);
            userEmitters.remove(userId, emitter);
            redisMessageListenerContainer.removeMessageListener(listener);
        });

        emitter.onTimeout(() -> {
            log.info("SSE connection timed out for user {}", userId);
            userEmitters.remove(userId, emitter);
            redisMessageListenerContainer.removeMessageListener(listener);
        });

        emitter.onError(e -> {
            log.warn("SSE connection error for user {}: {}", userId, e.getMessage());
            userEmitters.remove(userId, emitter);
            redisMessageListenerContainer.removeMessageListener(listener);
        });

        // Send initial unread count
        try {
            long unreadCount = notificationService.getUnreadCount(userId, "sse-init");
            emitter.send(SseEmitter.event()
                    .name("init")
                    .data("{\"unreadCount\":" + unreadCount + "}"));
        } catch (IOException e) {
            log.warn("Error sending initial SSE data to user {}: {}", userId, e.getMessage());
        }

        return emitter;
    }

    private UUID getCurrentUserId(UserDetails userDetails) {
        if (userDetails instanceof AuthenticationUserDetails aud) {
            return aud.getUserId();
        }
        throw new IllegalStateException("Unexpected principal type: " + userDetails.getClass().getName());
    }
}
