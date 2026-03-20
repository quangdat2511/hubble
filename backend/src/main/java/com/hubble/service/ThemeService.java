package com.hubble.service;

import com.hubble.entity.UserSettings;
import com.hubble.repository.UserSettingsRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class ThemeService {

    private final UserSettingsRepository repo;

    public ThemeService(UserSettingsRepository repo) {
        this.repo = repo;
    }

    public String getTheme(UUID userId) {
        return repo.findById(userId)
                .map(UserSettings::getTheme)
                .orElse("light"); // default
    }

    public void updateTheme(UUID userId, String theme) {
        UserSettings settings = repo.findById(userId)
                .orElse(new UserSettings());

        settings.setUserId(userId);
        settings.setTheme(theme);
        settings.setUpdatedAt(LocalDateTime.now());

        repo.save(settings);
    }
}
