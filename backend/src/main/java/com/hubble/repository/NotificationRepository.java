package com.hubble.repository;

import com.hubble.entity.Notification;
import com.hubble.enums.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    Page<Notification> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    long countByUserIdAndIsReadFalse(UUID userId);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.userId = :userId AND n.isRead = false")
    void markAllAsReadByUserId(UUID userId);

    // Check if a notification already exists for a specific user/type/reference
    // Used to prevent duplicate notifications (e.g., duplicate friend request notifications)
    @Query("SELECT n FROM Notification n WHERE n.userId = :userId AND n.type = :type AND n.referenceId = :referenceId ORDER BY n.createdAt DESC LIMIT 1")
    Optional<Notification> findRecentNotification(UUID userId, NotificationType type, String referenceId);

    // Delete notifications for a specific user/type/reference combination
    // Used to clean up old notifications when unblocking users
    @Modifying
    @Query("DELETE FROM Notification n WHERE n.userId = :userId AND n.type = :type AND n.referenceId = :referenceId")
    void deleteByUserIdAndTypeAndReferenceId(UUID userId, NotificationType type, String referenceId);
}