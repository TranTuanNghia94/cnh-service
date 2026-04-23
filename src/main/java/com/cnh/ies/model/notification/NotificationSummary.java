package com.cnh.ies.model.notification;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationSummary {

    private long totalCount;
    private long unreadCount;
    private List<NotificationInfo> notifications;
}
