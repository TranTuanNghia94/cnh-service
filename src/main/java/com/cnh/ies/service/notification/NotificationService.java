package com.cnh.ies.service.notification;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.cnh.ies.entity.auth.UserEntity;
import com.cnh.ies.entity.notification.NotificationEntity;
import com.cnh.ies.exception.ApiException;
import com.cnh.ies.model.notification.CreateNotificationRequest;
import com.cnh.ies.model.notification.NotificationInfo;
import com.cnh.ies.model.notification.NotificationSummary;
import com.cnh.ies.repository.auth.UserRepo;
import com.cnh.ies.repository.notification.NotificationRepo;
import com.cnh.ies.util.RequestContext;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    private final NotificationRepo notificationRepo;
    private final UserRepo userRepo;

    public NotificationSummary getNotifications(UUID userId, int page, int limit, String requestId) {
        log.info("Getting notifications for user {} page={} limit={} [rid={}]", userId, page, limit, requestId);

        Pageable pageable = PageRequest.of(page, limit);
        Page<NotificationEntity> notificationPage = notificationRepo.findByUserId(userId, pageable);
        long unreadCount = notificationRepo.countUnreadByUserId(userId);

        List<NotificationInfo> notifications = notificationPage.getContent().stream()
                .map(this::toNotificationInfo)
                .toList();

        return NotificationSummary.builder()
                .totalCount(notificationPage.getTotalElements())
                .unreadCount(unreadCount)
                .notifications(notifications)
                .build();
    }

    public List<NotificationInfo> getUnreadNotifications(UUID userId, String requestId) {
        log.info("Getting unread notifications for user {} [rid={}]", userId, requestId);

        List<NotificationEntity> notifications = notificationRepo.findUnreadByUserId(userId);
        return notifications.stream()
                .map(this::toNotificationInfo)
                .toList();
    }

    public long getUnreadCount(UUID userId, String requestId) {
        return notificationRepo.countUnreadByUserId(userId);
    }

    @Transactional
    public void markAsRead(UUID notificationId, UUID userId, String requestId) {
        log.info("Marking notification {} as read for user {} [rid={}]", notificationId, userId, requestId);

        int updated = notificationRepo.markAsRead(notificationId, userId);
        if (updated == 0) {
            throw new ApiException(ApiException.ErrorCode.NOT_FOUND, "Notification not found",
                    HttpStatus.NOT_FOUND.value(), requestId);
        }
    }

    @Transactional
    public int markAllAsRead(UUID userId, String requestId) {
        log.info("Marking all notifications as read for user {} [rid={}]", userId, requestId);
        return notificationRepo.markAllAsReadByUserId(userId);
    }

    @Transactional
    public List<NotificationInfo> createNotifications(CreateNotificationRequest request, String requestId) {
        log.info("Creating notifications for {} users [rid={}]", 
                request.getUserIds() != null ? request.getUserIds().size() : 0, requestId);

        if (request.getUserIds() == null || request.getUserIds().isEmpty()) {
            throw new ApiException(ApiException.ErrorCode.BAD_REQUEST, "User IDs are required",
                    HttpStatus.BAD_REQUEST.value(), requestId);
        }

        List<UUID> userIds = request.getUserIds().stream()
                .map(UUID::fromString)
                .toList();

        List<UserEntity> users = userRepo.findAllById(userIds);
        if (users.isEmpty()) {
            throw new ApiException(ApiException.ErrorCode.NOT_FOUND, "No users found",
                    HttpStatus.NOT_FOUND.value(), requestId);
        }

        List<NotificationEntity> notifications = new ArrayList<>();
        String createdBy = RequestContext.getCurrentUsername();

        for (UserEntity user : users) {
            NotificationEntity notification = new NotificationEntity();
            notification.setUser(user);
            notification.setTitle(request.getTitle());
            notification.setMessage(request.getMessage());
            notification.setType(request.getType() != null ? request.getType() : NotificationType.INFO);
            notification.setCategory(request.getCategory());
            notification.setReferenceId(request.getReferenceId());
            notification.setReferenceType(request.getReferenceType());
            notification.setActionUrl(request.getActionUrl());
            notification.setPriority(request.getPriority() != null ? request.getPriority() : NotificationPriority.NORMAL);
            notification.setMetadata(request.getMetadata());
            notification.setIsRead(false);
            notification.setIsDeleted(false);
            notification.setCreatedBy(createdBy);
            notification.setUpdatedBy(createdBy);
            notifications.add(notification);
        }

        List<NotificationEntity> saved = notificationRepo.saveAll(notifications);
        log.info("Created {} notifications [rid={}]", saved.size(), requestId);

        return saved.stream()
                .map(this::toNotificationInfo)
                .toList();
    }

    public void sendNotification(UUID userId, String title, String message, String type, 
            String category, String referenceId, String referenceType, String actionUrl) {
        log.info("Sending notification to user {}: {}", userId, title);

        UserEntity user = userRepo.findById(userId).orElse(null);
        if (user == null) {
            log.warn("User {} not found, skipping notification", userId);
            return;
        }

        NotificationEntity notification = new NotificationEntity();
        notification.setUser(user);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setType(type != null ? type : NotificationType.INFO);
        notification.setCategory(category);
        notification.setReferenceId(referenceId);
        notification.setReferenceType(referenceType);
        notification.setActionUrl(actionUrl);
        notification.setPriority(NotificationPriority.NORMAL);
        notification.setIsRead(false);
        notification.setIsDeleted(false);
        notification.setCreatedBy("SYSTEM");
        notification.setUpdatedBy("SYSTEM");

        notificationRepo.save(notification);
    }

    public void sendNotificationToUsers(List<UUID> userIds, String title, String message, 
            String type, String category, String referenceId, String referenceType, String actionUrl) {
        log.info("Sending notification to {} users: {}", userIds.size(), title);

        List<UserEntity> users = userRepo.findAllById(userIds);
        List<NotificationEntity> notifications = new ArrayList<>();

        for (UserEntity user : users) {
            NotificationEntity notification = new NotificationEntity();
            notification.setUser(user);
            notification.setTitle(title);
            notification.setMessage(message);
            notification.setType(type != null ? type : NotificationType.INFO);
            notification.setCategory(category);
            notification.setReferenceId(referenceId);
            notification.setReferenceType(referenceType);
            notification.setActionUrl(actionUrl);
            notification.setPriority(NotificationPriority.NORMAL);
            notification.setIsRead(false);
            notification.setIsDeleted(false);
            notification.setCreatedBy("SYSTEM");
            notification.setUpdatedBy("SYSTEM");
            notifications.add(notification);
        }

        notificationRepo.saveAll(notifications);
        log.info("Sent {} notifications", notifications.size());
    }

    @Transactional
    public void deleteNotification(UUID notificationId, UUID userId, String requestId) {
        log.info("Deleting notification {} for user {} [rid={}]", notificationId, userId, requestId);

        NotificationEntity notification = notificationRepo.findById(notificationId)
                .orElseThrow(() -> new ApiException(ApiException.ErrorCode.NOT_FOUND, "Notification not found",
                        HttpStatus.NOT_FOUND.value(), requestId));

        if (!notification.getUser().getId().equals(userId)) {
            throw new ApiException(ApiException.ErrorCode.FORBIDDEN, "Cannot delete other user's notification",
                    HttpStatus.FORBIDDEN.value(), requestId);
        }

        notification.setIsDeleted(true);
        notification.setUpdatedBy(RequestContext.getCurrentUsername());
        notificationRepo.save(notification);
    }

    private NotificationInfo toNotificationInfo(NotificationEntity entity) {
        return NotificationInfo.builder()
                .id(entity.getId().toString())
                .userId(entity.getUser().getId().toString())
                .title(entity.getTitle())
                .message(entity.getMessage())
                .type(entity.getType())
                .category(entity.getCategory())
                .referenceId(entity.getReferenceId())
                .referenceType(entity.getReferenceType())
                .actionUrl(entity.getActionUrl())
                .isRead(entity.getIsRead())
                .readAt(formatInstant(entity.getReadAt()))
                .priority(entity.getPriority())
                .metadata(entity.getMetadata())
                .createdAt(formatInstant(entity.getCreatedAt()))
                .build();
    }

    private String formatInstant(Instant instant) {
        return instant != null ? DATE_FORMATTER.format(instant) : null;
    }

    public static class NotificationType {
        public static final String INFO = "INFO";
        public static final String SUCCESS = "SUCCESS";
        public static final String WARNING = "WARNING";
        public static final String ERROR = "ERROR";
        public static final String PAYMENT = "PAYMENT";
        public static final String APPROVAL = "APPROVAL";
        public static final String ORDER = "ORDER";
        public static final String SYSTEM = "SYSTEM";

        private NotificationType() {}
    }

    public static class NotificationPriority {
        public static final String LOW = "LOW";
        public static final String NORMAL = "NORMAL";
        public static final String HIGH = "HIGH";
        public static final String URGENT = "URGENT";

        private NotificationPriority() {}
    }

    public static class NotificationCategory {
        public static final String PAYMENT_REQUEST = "PAYMENT_REQUEST";
        public static final String PURCHASE_ORDER = "PURCHASE_ORDER";
        public static final String SALES_ORDER = "SALES_ORDER";
        public static final String APPROVAL = "APPROVAL";
        public static final String SYSTEM = "SYSTEM";

        private NotificationCategory() {}
    }

    public static class ReferenceType {
        public static final String PAYMENT_REQUEST = "PAYMENT_REQUEST";
        public static final String PURCHASE_ORDER = "PURCHASE_ORDER";
        public static final String SALES_ORDER = "SALES_ORDER";
        public static final String USER = "USER";

        private ReferenceType() {}
    }
}
