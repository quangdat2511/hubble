package com.hubble.service;

import com.hubble.dto.response.SessionResponse;
import com.hubble.entity.UserSession;
import com.hubble.enums.DeviceType;
import com.hubble.exception.AppException;
import com.hubble.exception.ErrorCode;
import com.hubble.repository.UserSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SessionServiceTest {

    @Mock
    private UserSessionRepository userSessionRepository;

    @InjectMocks
    private SessionService sessionService;

    private UUID userId;
    private UUID sessionId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        sessionId = UUID.randomUUID();
    }

    @Test
    void getActiveSessions_ReturnsSessionList() {
        UserSession session1 = UserSession.builder()
                .id(sessionId)
                .userId(userId)
                .deviceName("iPhone 13")
                .deviceType(DeviceType.MOBILE)
                .ipAddress("10.0.0.1")
                .isActive(true)
                .lastActiveAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();

        UserSession session2 = UserSession.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .deviceName("MacBook Pro")
                .deviceType(DeviceType.DESKTOP)
                .ipAddress("10.0.0.2")
                .isActive(true)
                .lastActiveAt(LocalDateTime.now().minusDays(1))
                .createdAt(LocalDateTime.now().minusDays(1))
                .build();

        when(userSessionRepository.findAllByUserIdAndIsActiveTrueOrderByLastActiveAtDesc(userId))
                .thenReturn(List.of(session1, session2));

        List<SessionResponse> responses = sessionService.getActiveSessions(userId);

        assertEquals(2, responses.size());

        assertEquals(sessionId, responses.get(0).getId());
        assertEquals("iPhone 13", responses.get(0).getDeviceName());
        assertEquals("MOBILE", responses.get(0).getDeviceType());

        assertEquals("MacBook Pro", responses.get(1).getDeviceName());
        assertEquals("DESKTOP", responses.get(1).getDeviceType());
    }

    @Test
    void getActiveSessions_NoActiveSessions_ReturnsEmptyList() {
        when(userSessionRepository.findAllByUserIdAndIsActiveTrueOrderByLastActiveAtDesc(userId))
                .thenReturn(List.of());

        List<SessionResponse> responses = sessionService.getActiveSessions(userId);

        assertTrue(responses.isEmpty());
    }

    @Test
    void revokeSession_SessionExists_DeactivatesSession() {
        UserSession session = UserSession.builder()
                .id(sessionId)
                .userId(userId)
                .isActive(true)
                .build();

        when(userSessionRepository.findByIdAndUserIdAndIsActiveTrue(sessionId, userId))
                .thenReturn(Optional.of(session));

        sessionService.revokeSession(userId, sessionId);

        ArgumentCaptor<UserSession> sessionCaptor = ArgumentCaptor.forClass(UserSession.class);
        verify(userSessionRepository).save(sessionCaptor.capture());

        UserSession savedSession = sessionCaptor.getValue();
        assertFalse(savedSession.getIsActive());
    }

    @Test
    void revokeSession_SessionNotFoundOrNotActive_ThrowsException() {
        when(userSessionRepository.findByIdAndUserIdAndIsActiveTrue(sessionId, userId))
                .thenReturn(Optional.empty());

        AppException exception = assertThrows(AppException.class,
                () -> sessionService.revokeSession(userId, sessionId));

        assertEquals(ErrorCode.NOT_FOUND, exception.getErrorCode());
        verify(userSessionRepository, never()).save(any(UserSession.class));
    }
}