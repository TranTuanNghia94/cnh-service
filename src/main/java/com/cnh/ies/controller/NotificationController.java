package com.cnh.ies.controller;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cnh.ies.dto.common.ApiResponse;
import com.cnh.ies.entity.auth.UserEntity;
import com.cnh.ies.model.notification.CreateNotificationRequest;
import com.cnh.ies.model.notification.NotificationInfo;
import com.cnh.ies.model.notification.NotificationSummary;
import com.cnh.ies.repository.auth.UserRepo;
import com.cnh.ies.service.notification.NotificationService;
import com.cnh.ies.util.RequestContext;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final UserRepo userRepo;

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

    @PutMapping("/{id}/read")
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

    private UUID getCurrentUserId(UserDetails userDetails) {
        UserEntity user = userRepo.findOneByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        return user.getId();
    }
}
