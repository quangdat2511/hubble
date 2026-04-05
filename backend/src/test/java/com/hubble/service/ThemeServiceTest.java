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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ThemeServiceTest {

    @Mock
    private UserSettingsRepository userSettingsRepository;

    @InjectMocks
    private ThemeService themeService;

    @Test
    void getTheme_NoSettingsRow_ReturnsDark() {
        UUID userId = UUID.randomUUID();
        when(userSettingsRepository.findById(userId)).thenReturn(Optional.empty());

        String result = themeService.getTheme(userId);

        assertEquals("DARK", result);
    }

    @Test
    void getTheme_StoredThemeIsNormalized() {
        UUID userId = UUID.randomUUID();
        UserSettings settings = UserSettings.builder()
                .userId(userId)
                .theme(" LIGHT ")
                .build();
        when(userSettingsRepository.findById(userId)).thenReturn(Optional.of(settings));

        String result = themeService.getTheme(userId);

        assertEquals("LIGHT", result);
    }

    @Test
    void getTheme_InvalidStoredTheme_FallsBackToDark() {
        UUID userId = UUID.randomUUID();
        UserSettings settings = UserSettings.builder()
                .userId(userId)
                .theme("solarized")
                .build();
        when(userSettingsRepository.findById(userId)).thenReturn(Optional.of(settings));

        String result = themeService.getTheme(userId);

        assertEquals("DARK", result);
    }

    @Test
    void updateTheme_InvalidTheme_ThrowsAppException() {
        UUID userId = UUID.randomUUID();

        AppException exception = assertThrows(AppException.class,
                () -> themeService.updateTheme(userId, "system"));

        assertEquals(ErrorCode.INVALID_THEME, exception.getErrorCode());
    }

    @Test
    void updateTheme_MissingSettingsRow_CreatesNewRowWithNormalizedTheme() {
        UUID userId = UUID.randomUUID();
        when(userSettingsRepository.findById(userId)).thenReturn(Optional.empty());
        when(userSettingsRepository.save(any(UserSettings.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        String result = themeService.updateTheme(userId, " DARK ");

        ArgumentCaptor<UserSettings> settingsCaptor = ArgumentCaptor.forClass(UserSettings.class);
        verify(userSettingsRepository).save(settingsCaptor.capture());

        UserSettings savedSettings = settingsCaptor.getValue();
        assertEquals("DARK", result);
        assertEquals(userId, savedSettings.getUserId());
        assertEquals("DARK", savedSettings.getTheme());
    }
}
