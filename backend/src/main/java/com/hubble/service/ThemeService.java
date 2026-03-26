package com.hubble.service;

import com.hubble.entity.UserSettings;
import com.hubble.exception.AppException;
import com.hubble.exception.ErrorCode;
import com.hubble.repository.UserSettingsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;

@Service
public class ThemeService {

    private static final String DEFAULT_THEME = "DARK";
    private static final String LIGHT_THEME = "LIGHT";

    private final UserSettingsRepository repo;

    public ThemeService(UserSettingsRepository repo) {
        this.repo = repo;
    }

    @Transactional(readOnly = true)
    public String getTheme(UUID userId) {
        return repo.findById(userId)
                .map(UserSettings::getTheme)
                .map(this::normalizeStoredTheme)
                .orElse(DEFAULT_THEME);
    }

    @Transactional
    public String updateTheme(UUID userId, String theme) {
        String normalizedTheme = normalizeRequestedTheme(theme);
        LocalDateTime updatedAt = LocalDateTime.now();
        int updatedRows = repo.updateThemeByUserId(userId, normalizedTheme, updatedAt);
        if (updatedRows > 0) {
            return normalizedTheme;
        }

        UserSettings settings = buildDefaultSettings(userId);
        settings.setTheme(normalizedTheme);
        settings.setUpdatedAt(updatedAt);

        UserSettings savedSettings = repo.saveAndFlush(settings);
        return normalizeStoredTheme(savedSettings.getTheme());
    }

    private String normalizeRequestedTheme(String theme) {
        if (theme == null) {
            throw new AppException(ErrorCode.INVALID_THEME);
        }

        String normalizedTheme = theme.trim().toUpperCase(Locale.ROOT);
        if (!isSupportedTheme(normalizedTheme)) {
            throw new AppException(ErrorCode.INVALID_THEME);
        }

        return normalizedTheme;
    }

    private String normalizeStoredTheme(String theme) {
        if (theme == null) {
            return DEFAULT_THEME;
        }

        String normalizedTheme = theme.trim().toUpperCase(Locale.ROOT);
        return isSupportedTheme(normalizedTheme) ? normalizedTheme : DEFAULT_THEME;
    }

    private boolean isSupportedTheme(String theme) {
        return LIGHT_THEME.equals(theme) || DEFAULT_THEME.equals(theme);
    }

    private UserSettings buildDefaultSettings(UUID userId) {
        return UserSettings.builder()
                .userId(userId)
                .theme(DEFAULT_THEME)
                .locale("vi")
                .notificationEnabled(true)
                .notificationSound(true)
                .updatedAt(LocalDateTime.now())
                .build();
    }
}
