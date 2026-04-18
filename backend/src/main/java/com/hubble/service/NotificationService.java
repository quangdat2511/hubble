package com.hubble.service;

import com.hubble.dto.response.NotificationResponse;
import com.hubble.entity.Notification;
import com.hubble.enums.NotificationType;
import com.hubble.exception.AppException;
import com.hubble.exception.ErrorCode;
import com.hubble.mapper.NotificationMapper;
import com.hubble.repository.NotificationRepository;
import com.hubble.repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class NotificationService {

    NotificationRepository notificationRepository;
    UserRepository userRepository;
    NotificationMapper notificationMapper;
    SimpMessagingTemplate messagingTemplate;
    EmailService emailService;
    PushNotificationService pushNotificationService;

    @Transactional
    public void dispatchNotification(UUID userId, NotificationType type, String referenceId, String content, boolean sendEmail, boolean sendPush) {
        log.info("Dispatching {} notification to user {} | Content: {}", type, userId, content);

        if (type == NotificationType.FRIEND_REQUEST && referenceId != null) {
            Optional<Notification> existing = notificationRepository.findRecentNotification(userId, type, referenceId);
            if (existing.isPresent()) {
                Notification notif = existing.get();
                // If a similar notification was created within last 60 seconds, skip
                long secondsAgo = java.time.temporal.ChronoUnit.SECONDS.between(
                    notif.getCreatedAt(), java.time.LocalDateTime.now());
                if (secondsAgo < 60) {
                    log.info("Skipping duplicate {} notification for user {} (created {} seconds ago)", 
                        type, userId, secondsAgo);
                    return;
                }
            }
        }
        
        Notification notification = Notification.builder()
                .userId(userId)
                .type(type)
                .referenceId(referenceId)
                .content(content)
                .isRead(false)
                .build();

        notification = notificationRepository.save(notification);
        NotificationResponse response = notificationMapper.toResponse(notification);

        messagingTemplate.convertAndSend("/topic/users/" + userId + "/notifications", response);

        if (sendPush) {
            log.info("Sending push notification to user {}", userId);
            pushNotificationService.sendPushNotification(userId, content, content);
        }

        if (sendEmail) {
            userRepository.findById(userId).ifPresent(user -> {
                if (user.getEmail() != null) {
                    emailService.sendNotificationEmail(user.getEmail(), "Hubble: Thông báo mới", content);
                }
            });
        }
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getUserNotifications(UUID userId, int page, int size) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size))
                .stream()
                .map(notificationMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(UUID userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    @Transactional
    public void markAsRead(UUID userId, UUID notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));

        if (!notification.getUserId().equals(userId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        if (Boolean.TRUE.equals(notification.getIsRead())) {
            return;
        }

        notification.setIsRead(true);
        notificationRepository.save(notification);
    }

    @Transactional
    public void markAllAsRead(UUID userId) {
        notificationRepository.markAllAsReadByUserId(userId);
    }
}