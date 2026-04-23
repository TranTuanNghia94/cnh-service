package com.cnh.ies.model.notification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationInfo {

    private String id;
    private String userId;
    private String title;
    private String message;
    private String type;
    private String category;
    private String referenceId;
    private String referenceType;
    private String actionUrl;
    private Boolean isRead;
    private String readAt;
    private String priority;
    private String metadata;
    private String createdAt;
}
