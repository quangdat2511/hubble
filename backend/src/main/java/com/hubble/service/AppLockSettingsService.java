package com.hubble.service;

import com.hubble.dto.response.AppLockSettingsResponse;
import com.hubble.entity.UserSettings;
import com.hubble.exception.AppException;
import com.hubble.exception.ErrorCode;
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
public class AppLockSettingsService {

    int PIN_LENGTH = 4;
    UserSettingsRepository repo;

    public AppLockSettingsResponse getAppLockSettings(UUID userId) {
        UserSettings settings = repo.findById(userId)
                .orElseGet(() -> buildDefaultSettings(userId));
        return mapToResponse(settings);
    }

    public AppLockSettingsResponse updateAppLockPin(UUID userId, String pin) {
        UserSettings settings = repo.findById(userId)
                .orElseGet(() -> buildDefaultSettings(userId));

        settings.setAppLockPin(normalizePin(pin));
        settings.setUpdatedAt(LocalDateTime.now());

        return mapToResponse(repo.save(settings));
    }

    private AppLockSettingsResponse mapToResponse(UserSettings settings) {
        String appLockPin = normalizeStoredPin(settings.getAppLockPin());
        return AppLockSettingsResponse.builder()
                .appLockPin(appLockPin)
                .pinConfigured(appLockPin != null)
                .build();
    }

    private String normalizePin(String pin) {
        if (pin == null) {
            return null;
        }

        String normalizedPin = pin.trim();
        if (normalizedPin.isEmpty()) {
            return null;
        }

        if (!normalizedPin.matches("\\d{" + PIN_LENGTH + "}")) {
            throw new AppException(ErrorCode.INVALID_APP_LOCK_PIN);
        }
        return normalizedPin;
    }

    private String normalizeStoredPin(String pin) {
        if (pin == null) {
            return null;
        }

        String normalizedPin = pin.trim();
        return normalizedPin.matches("\\d{" + PIN_LENGTH + "}") ? normalizedPin : null;
    }

    private UserSettings buildDefaultSettings(UUID userId) {
        return UserSettings.builder()
                .userId(userId)
                .theme("SYSTEM")
                .locale("vi")
                .notificationEnabled(true)
                .notificationSound(true)
                .updatedAt(LocalDateTime.now())
                .build();
    }
}
