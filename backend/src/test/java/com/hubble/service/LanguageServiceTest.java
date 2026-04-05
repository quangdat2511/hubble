package com.hubble.service;

import com.hubble.entity.UserSettings;
import com.hubble.exception.AppException;
import com.hubble.exception.ErrorCode;
import com.hubble.repository.UserSettingsRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LanguageServiceTest {

    @Mock
    private UserSettingsRepository userSettingsRepository;

    @InjectMocks
    private LanguageService languageService;

    @Test
    void getLanguage_ReturnsDefaultLocaleWhenMissing() {
        UUID userId = UUID.randomUUID();
        when(userSettingsRepository.findById(userId)).thenReturn(Optional.empty());

        String locale = languageService.getLanguage(userId);

        assertEquals("vi", locale);
    }

    @Test
    void getLanguage_NormalizesStoredLocale() {
        UUID userId = UUID.randomUUID();
        UserSettings settings = UserSettings.builder()
                .userId(userId)
                .locale("EN")
                .build();
        when(userSettingsRepository.findById(userId)).thenReturn(Optional.of(settings));

        String locale = languageService.getLanguage(userId);

        assertEquals("en", locale);
    }

    @Test
    void updateLanguage_NormalizesAndPersistsLocale() {
        UUID userId = UUID.randomUUID();
        when(userSettingsRepository.findById(userId)).thenReturn(Optional.empty());

        languageService.updateLanguage(userId, " VI ");

        ArgumentCaptor<UserSettings> settingsCaptor = ArgumentCaptor.forClass(UserSettings.class);
        verify(userSettingsRepository).save(settingsCaptor.capture());
        assertEquals("vi", settingsCaptor.getValue().getLocale());
        assertEquals(userId, settingsCaptor.getValue().getUserId());
    }

    @Test
    void updateLanguage_RejectsUnsupportedLocale() {
        UUID userId = UUID.randomUUID();

        AppException exception = assertThrows(AppException.class,
                () -> languageService.updateLanguage(userId, "fr"));

        assertEquals(ErrorCode.LOCALE_INVALID, exception.getErrorCode());
    }
}
