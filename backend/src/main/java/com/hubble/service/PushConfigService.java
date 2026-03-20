package com.hubble.service;

import com.hubble.entity.UserSettings;
import com.hubble.repository.UserSettingsRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class PushConfigService {

    private final UserSettingsRepository repo;

    public PushConfigService(UserSettingsRepository repo) {
        this.repo = repo;
    }

    public UserSettings getPushConfig(UUID userId) {
        return repo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User settings not found"));
    }

    public void updatePushConfig(UUID userId, Boolean enabled, Boolean sound) {
        UserSettings settings = repo.findById(userId)
                .orElse(new UserSettings());

        settings.setUserId(userId);
        settings.setNotificationEnabled(enabled);
        settings.setNotificationSound(sound);
        settings.setUpdatedAt(LocalDateTime.now());

        repo.save(settings);
    }
}