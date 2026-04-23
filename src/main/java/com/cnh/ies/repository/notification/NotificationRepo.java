package com.cnh.ies.repository.notification;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.cnh.ies.entity.notification.NotificationEntity;
import com.cnh.ies.repository.BaseRepo;

@Repository
public interface NotificationRepo extends BaseRepo<NotificationEntity, UUID> {

    @Query("SELECT n FROM NotificationEntity n WHERE n.user.id = :userId AND n.isDeleted = false ORDER BY n.createdAt DESC")
    Page<NotificationEntity> findByUserId(@Param("userId") UUID userId, Pageable pageable);

    @Query("SELECT n FROM NotificationEntity n WHERE n.user.id = :userId AND n.isRead = false AND n.isDeleted = false ORDER BY n.createdAt DESC")
    List<NotificationEntity> findUnreadByUserId(@Param("userId") UUID userId);

    @Query("SELECT COUNT(n) FROM NotificationEntity n WHERE n.user.id = :userId AND n.isRead = false AND n.isDeleted = false")
    long countUnreadByUserId(@Param("userId") UUID userId);

    @Modifying
    @Query("UPDATE NotificationEntity n SET n.isRead = true, n.readAt = CURRENT_TIMESTAMP WHERE n.user.id = :userId AND n.isRead = false AND n.isDeleted = false")
    int markAllAsReadByUserId(@Param("userId") UUID userId);

    @Modifying
    @Query("UPDATE NotificationEntity n SET n.isRead = true, n.readAt = CURRENT_TIMESTAMP WHERE n.id = :notificationId AND n.user.id = :userId AND n.isDeleted = false")
    int markAsRead(@Param("notificationId") UUID notificationId, @Param("userId") UUID userId);

    @Query("SELECT n FROM NotificationEntity n WHERE n.user.id = :userId AND n.category = :category AND n.isDeleted = false ORDER BY n.createdAt DESC")
    Page<NotificationEntity> findByUserIdAndCategory(@Param("userId") UUID userId, @Param("category") String category, Pageable pageable);

    @Query("SELECT n FROM NotificationEntity n WHERE n.referenceId = :referenceId AND n.referenceType = :referenceType AND n.isDeleted = false")
    List<NotificationEntity> findByReference(@Param("referenceId") String referenceId, @Param("referenceType") String referenceType);
}
