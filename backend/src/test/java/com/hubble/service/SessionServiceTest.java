package com.hubble.service;

import com.hubble.dto.response.SessionResponse;
import com.hubble.entity.UserSession;
import com.hubble.enums.DeviceType;
import com.hubble.exception.AppException;
import com.hubble.exception.ErrorCode;
import com.hubble.repository.UserSessionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
class SessionServiceTest {

    @Mock
    private UserSessionRepository userSessionRepository;

    @InjectMocks
    private SessionService sessionService;

    @Test
    void getActiveSessions_Success_WithDeviceType() {
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();

        UserSession session = UserSession.builder()
                .id(sessionId)
                .userId(userId)
                .deviceName("iPhone 14 Pro")
                .deviceType(DeviceType.MOBILE)
                .ipAddress("192.168.1.100")
                .isActive(true)
                .lastActiveAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now().minusDays(1))
                .build();

        when(userSessionRepository.findAllByUserIdAndIsActiveTrueOrderByLastActiveAtDesc(userId))
                .thenReturn(List.of(session));

        List<SessionResponse> responses = sessionService.getActiveSessions(userId);

        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertEquals(sessionId, responses.get(0).getId());
        assertEquals("iPhone 14 Pro", responses.get(0).getDeviceName());
        assertEquals(DeviceType.MOBILE.name(), responses.get(0).getDeviceType());
        assertEquals("192.168.1.100", responses.get(0).getIpAddress());

        verify(userSessionRepository, times(1)).findAllByUserIdAndIsActiveTrueOrderByLastActiveAtDesc(userId);
    }

    @Test
    void getActiveSessions_Success_WithNullDeviceType() {
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();

        UserSession session = UserSession.builder()
                .id(sessionId)
                .userId(userId)
                .deviceName("Unknown Device")
                .deviceType(null)
                .ipAddress("10.0.0.1")
                .isActive(true)
                .lastActiveAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now().minusDays(2))
                .build();

        when(userSessionRepository.findAllByUserIdAndIsActiveTrueOrderByLastActiveAtDesc(userId))
                .thenReturn(List.of(session));

        List<SessionResponse> responses = sessionService.getActiveSessions(userId);

        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertEquals(sessionId, responses.get(0).getId());
        assertEquals("Unknown Device", responses.get(0).getDeviceName());
        assertNull(responses.get(0).getDeviceType());
        assertEquals("10.0.0.1", responses.get(0).getIpAddress());

        verify(userSessionRepository, times(1)).findAllByUserIdAndIsActiveTrueOrderByLastActiveAtDesc(userId);
    }

    @Test
    void getActiveSessions_EmptyList_Success() {
        UUID userId = UUID.randomUUID();

        when(userSessionRepository.findAllByUserIdAndIsActiveTrueOrderByLastActiveAtDesc(userId))
                .thenReturn(List.of());

        List<SessionResponse> responses = sessionService.getActiveSessions(userId);

        assertNotNull(responses);
        assertTrue(responses.isEmpty());

        verify(userSessionRepository, times(1)).findAllByUserIdAndIsActiveTrueOrderByLastActiveAtDesc(userId);
    }

    @Test
    void revokeSession_Success() {
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();

        UserSession session = UserSession.builder()
                .id(sessionId)
                .userId(userId)
                .isActive(true)
                .build();

        when(userSessionRepository.findByIdAndUserIdAndIsActiveTrue(sessionId, userId))
                .thenReturn(Optional.of(session));

        sessionService.revokeSession(userId, sessionId);

        assertFalse(session.getIsActive());
        verify(userSessionRepository, times(1)).save(session);
    }

    @Test
    void revokeSession_SessionNotFoundOrNotActive_ThrowsAppException() {
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();

        when(userSessionRepository.findByIdAndUserIdAndIsActiveTrue(sessionId, userId))
                .thenReturn(Optional.empty());

        AppException exception = assertThrows(AppException.class, () -> sessionService.revokeSession(userId, sessionId));

        assertEquals(ErrorCode.NOT_FOUND, exception.getErrorCode());
        verify(userSessionRepository, never()).save(any(UserSession.class));
    }
}