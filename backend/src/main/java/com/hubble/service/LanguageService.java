package com.hubble.service;

import com.hubble.entity.UserSettings;
import com.hubble.repository.UserSettingsRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class LanguageService {

    private final UserSettingsRepository repo;

    public LanguageService(UserSettingsRepository repo) {
        this.repo = repo;
    }

    public String getLanguage(UUID userId) {
        return repo.findById(userId)
                .map(UserSettings::getLocale)
                .orElse("en"); // default
    }

    public void updateLanguage(UUID userId, String locale) {
        UserSettings settings = repo.findById(userId)
                .orElse(new UserSettings());

        settings.setUserId(userId);
        settings.setLocale(locale);
        settings.setUpdatedAt(LocalDateTime.now());

        repo.save(settings);
    }
}
