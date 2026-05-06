package com.hubble.service;

import com.hubble.dto.request.PushConfigUpdateRequest;
import com.hubble.dto.response.PushConfigResponse;
import com.hubble.entity.UserSettings;
import com.hubble.repository.UserSettingsRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PushConfigService {

    UserSettingsRepository repo;

    public PushConfigResponse getPushConfig(UUID userId) {
        UserSettings settings = repo.findById(userId)
                .orElseGet(() -> buildDefaultSettings(userId));

        return mapToResponse(settings);
    }

    public PushConfigResponse updatePushConfig(UUID userId, PushConfigUpdateRequest request) {
        UserSettings settings = repo.findById(userId)
                .orElseGet(() -> buildDefaultSettings(userId));

        if (request.getNotificationEnabled() != null) {
            settings.setNotificationEnabled(request.getNotificationEnabled());
        }
        if (request.getNotificationSound() != null) {
            settings.setNotificationSound(request.getNotificationSound());
        }
        settings.setUpdatedAt(LocalDateTime.now());

        UserSettings savedSettings = repo.save(settings);
        return mapToResponse(savedSettings);
    }

    private PushConfigResponse mapToResponse(UserSettings settings) {
        return PushConfigResponse.builder()
                .notificationEnabled(Boolean.TRUE.equals(settings.getNotificationEnabled()))
                .notificationSound(Boolean.TRUE.equals(settings.getNotificationSound()))
                .build();
    }

    private UserSettings buildDefaultSettings(UUID userId) {
        return UserSettings.builder()
                .userId(userId)
                .theme("SYSTEM")
                .locale("vi")
                .notificationEnabled(true)
                .notificationSound(true)
                .newDeviceLoginAlertsEnabled(true)
                .updatedAt(LocalDateTime.now())
                .build();
    }
}
