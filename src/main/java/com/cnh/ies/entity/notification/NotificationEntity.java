package com.cnh.ies.entity.notification;

import java.time.Instant;

import com.cnh.ies.entity.BaseEntity;
import com.cnh.ies.entity.auth.UserEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "notifications", indexes = {
    @Index(name = "idx_notification_user_read", columnList = "user_id, is_read"),
    @Index(name = "idx_notification_user_created", columnList = "user_id, created_at DESC")
})
@Data
@EqualsAndHashCode(callSuper = true)
public class NotificationEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "type", nullable = false, length = 50)
    private String type;

    @Column(name = "category", length = 50)
    private String category;

    @Column(name = "reference_id")
    private String referenceId;

    @Column(name = "reference_type", length = 50)
    private String referenceType;

    @Column(name = "action_url", length = 500)
    private String actionUrl;

    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;

    @Column(name = "read_at")
    private Instant readAt;

    @Column(name = "priority", length = 20)
    private String priority = "NORMAL";

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;
}
