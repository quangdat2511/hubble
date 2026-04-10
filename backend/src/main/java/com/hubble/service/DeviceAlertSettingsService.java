package com.hubble.service;

import com.hubble.dto.request.DeviceAlertSettingsUpdateRequest;
import com.hubble.dto.response.DeviceAlertSettingsResponse;
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
public class DeviceAlertSettingsService {

    UserSettingsRepository repo;

    public DeviceAlertSettingsResponse getSettings(UUID userId) {
        UserSettings settings = repo.findById(userId)
                .orElseGet(() -> buildDefaultSettings(userId));
        return mapToResponse(settings);
    }

    public DeviceAlertSettingsResponse updateSettings(UUID userId, DeviceAlertSettingsUpdateRequest request) {
        UserSettings settings = repo.findById(userId)
                .orElseGet(() -> buildDefaultSettings(userId));

        if (request.getEnabled() != null) {
            settings.setNewDeviceLoginAlertsEnabled(request.getEnabled());
        }
        settings.setUpdatedAt(LocalDateTime.now());

        return mapToResponse(repo.save(settings));
    }

    private DeviceAlertSettingsResponse mapToResponse(UserSettings settings) {
        return DeviceAlertSettingsResponse.builder()
                .enabled(!Boolean.FALSE.equals(settings.getNewDeviceLoginAlertsEnabled()))
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
