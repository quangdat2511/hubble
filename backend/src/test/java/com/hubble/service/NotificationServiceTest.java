package com.hubble.service;

import com.hubble.dto.response.NotificationResponse;
import com.hubble.entity.Notification;
import com.hubble.entity.User;
import com.hubble.enums.NotificationType;
import com.hubble.exception.AppException;
import com.hubble.mapper.NotificationMapper;
import com.hubble.repository.NotificationRepository;
import com.hubble.repository.UserRepository;
import com.hubble.testsupport.MessagingTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock private NotificationRepository notificationRepository;
    @Mock private UserRepository userRepository;
    @Mock private NotificationMapper notificationMapper;
    @Spy
    private SimpMessagingTemplate messagingTemplate = MessagingTestSupport.createTemplate();
    @Mock private EmailService emailService;
    @Mock private PushNotificationService pushNotificationService;

    @InjectMocks
    private NotificationService notificationService;

    private UUID userId;
    private UUID notificationId;
    private Notification notification;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        notificationId = UUID.randomUUID();
        notification = Notification.builder()
                .id(notificationId)
                .userId(userId)
                .content("Test Content")
                .isRead(false)
                .build();
    }

    @Test
    void dispatchNotification_NewRequest_ShouldSaveAndSend() {
        String refId = UUID.randomUUID().toString();

        // Removed unnecessary findRecentNotification stub
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);
        when(notificationMapper.toResponse(any())).thenReturn(new NotificationResponse());

        User user = new User();
        user.setEmail("test@gmail.com");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        lenient().doNothing().when(pushNotificationService).sendPushNotification(any(), any(), any());
        lenient().doNothing().when(emailService).sendNotificationEmail(any(), any(), any());

        notificationService.dispatchNotification(userId, NotificationType.FRIEND_REQUEST, refId, "Nội dung", true, true);

        // Verify the new cleanup logic is called
        verify(notificationRepository).deleteByUserIdAndTypeAndReferenceId(userId, NotificationType.FRIEND_REQUEST, refId);
        verify(notificationRepository).save(any(Notification.class));
        verify(messagingTemplate).convertAndSend(eq("/topic/users/" + userId + "/notifications"), any(NotificationResponse.class));
        verify(pushNotificationService).sendPushNotification(userId, "Nội dung", "Nội dung");
        verify(emailService).sendNotificationEmail("test@gmail.com", "Hubble: Thông báo mới", "Nội dung");
    }

    @Test
    void dispatchNotification_NullReferenceId_ShouldNotCleanUpOldNotifications() {
        // Replaces the old 60-second test to verify the updated business logic
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);
        when(notificationMapper.toResponse(any())).thenReturn(new NotificationResponse());
        lenient().when(userRepository.findById(userId)).thenReturn(Optional.of(new User()));

        notificationService.dispatchNotification(userId, NotificationType.FRIEND_REQUEST, null, "Nội dung", false, false);

        // If referenceId is null, cleanup should NOT be invoked
        verify(notificationRepository, never()).deleteByUserIdAndTypeAndReferenceId(any(), any(), any());
        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    void dispatchNotification_PushAndEmailDisabled_ShouldSaveButNotSendExternal() {
        String refId = UUID.randomUUID().toString();

        // Removed unnecessary findRecentNotification stub
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);
        when(notificationMapper.toResponse(any())).thenReturn(new NotificationResponse());
        lenient().when(userRepository.findById(userId)).thenReturn(Optional.of(new User()));
        lenient().doNothing().when(pushNotificationService).sendPushNotification(any(), any(), any());
        lenient().doNothing().when(emailService).sendNotificationEmail(any(), any(), any());

        notificationService.dispatchNotification(userId, NotificationType.FRIEND_REQUEST, refId, "Nội dung", false, false);

        // Verify cleanup happens
        verify(notificationRepository).deleteByUserIdAndTypeAndReferenceId(userId, NotificationType.FRIEND_REQUEST, refId);
        verify(notificationRepository).save(any(Notification.class));
        verify(messagingTemplate).convertAndSend(eq("/topic/users/" + userId + "/notifications"), any(NotificationResponse.class));

        verify(pushNotificationService, never()).sendPushNotification(any(), any(), any());
        verify(emailService, never()).sendNotificationEmail(any(), any(), any());
    }

    @Test
    void getUserNotifications_ShouldReturnList() {
        Page<Notification> page = new PageImpl<>(List.of(notification));
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(eq(userId), any(PageRequest.class)))
                .thenReturn(page);
        when(notificationMapper.toResponse(any())).thenReturn(new NotificationResponse());

        List<NotificationResponse> result = notificationService.getUserNotifications(userId, 0, 20);

        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
    }

    @Test
    void getUnreadCount_ShouldReturnCount() {
        when(notificationRepository.countByUserIdAndIsReadFalse(userId)).thenReturn(10L);
        long count = notificationService.getUnreadCount(userId);
        assertEquals(10L, count);
    }

    @Test
    void markAsRead_ValidOwner_ShouldUpdateIsRead() {
        when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(notification));

        notificationService.markAsRead(userId, notificationId);

        assertTrue(notification.getIsRead());
        verify(notificationRepository).save(notification);
    }

    @Test
    void markAsRead_InvalidOwner_ShouldThrowAppException() {
        notification.setUserId(UUID.randomUUID());
        when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(notification));

        assertThrows(AppException.class, () -> notificationService.markAsRead(userId, notificationId));
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void markAsRead_NotFound_ShouldThrowAppException() {
        when(notificationRepository.findById(notificationId)).thenReturn(Optional.empty());

        assertThrows(AppException.class, () -> notificationService.markAsRead(userId, notificationId));
    }

    @Test
    void markAllAsRead_ShouldCallRepository() {
        notificationService.markAllAsRead(userId);
        verify(notificationRepository).markAllAsReadByUserId(userId);
    }

    @Test
    void markAsRead_AlreadyRead_ShouldNotUpdateAgain() {
        notification.setIsRead(true);
        when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(notification));

        notificationService.markAsRead(userId, notificationId);

        verify(notificationRepository, never()).save(notification);
    }
}