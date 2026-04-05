package com.hubble.service;

import com.hubble.entity.UserSettings;
import com.hubble.exception.AppException;
import com.hubble.exception.ErrorCode;
import com.hubble.repository.UserSettingsRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class LanguageService {

    private static final String DEFAULT_LOCALE = "vi";
    private static final Set<String> SUPPORTED_LOCALES = Set.of("en", "vi");

    private final UserSettingsRepository repo;

    public LanguageService(UserSettingsRepository repo) {
        this.repo = repo;
    }

    public String getLanguage(UUID userId) {
        return repo.findById(userId)
                .map(UserSettings::getLocale)
                .filter(locale -> locale != null && !locale.isBlank())
                .map(locale -> locale.toLowerCase(Locale.ROOT))
                .orElse(DEFAULT_LOCALE);
    }

    public void updateLanguage(UUID userId, String locale) {
        String normalizedLocale = normalizeLocale(locale);

        UserSettings settings = repo.findById(userId)
                .orElseGet(() -> UserSettings.builder().userId(userId).build());

        settings.setLocale(normalizedLocale);
        settings.setUpdatedAt(LocalDateTime.now());

        repo.save(settings);
    }

    private String normalizeLocale(String locale) {
        if (locale == null) {
            throw new AppException(ErrorCode.LOCALE_INVALID);
        }

        String normalizedLocale = locale.trim().toLowerCase(Locale.ROOT);
        if (!SUPPORTED_LOCALES.contains(normalizedLocale)) {
            throw new AppException(ErrorCode.LOCALE_INVALID);
        }

        return normalizedLocale;
    }
}
