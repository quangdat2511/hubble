package com.hubble.service;

import com.hubble.dto.response.SessionResponse;
import com.hubble.entity.UserSession;
import com.hubble.exception.AppException;
import com.hubble.exception.ErrorCode;
import com.hubble.repository.UserSessionRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SessionService {

    UserSessionRepository userSessionRepository;

    public List<SessionResponse> getActiveSessions(UUID userId) {
        return userSessionRepository.findAllByUserIdAndIsActiveTrueOrderByLastActiveAtDesc(userId)
                .stream()
                .map(session -> SessionResponse.builder()
                        .id(session.getId())
                        .deviceName(session.getDeviceName())
                        .deviceType(session.getDeviceType() != null ? session.getDeviceType().name() : null)
                        .ipAddress(session.getIpAddress())
                        .lastActiveAt(session.getLastActiveAt())
                        .createdAt(session.getCreatedAt())
                        .build())
                .toList();
    }

    @Transactional
    public void revokeSession(UUID userId, UUID sessionId) {
        UserSession session = userSessionRepository.findByIdAndUserIdAndIsActiveTrue(sessionId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));

        session.setIsActive(false);
        userSessionRepository.save(session);
    }
}