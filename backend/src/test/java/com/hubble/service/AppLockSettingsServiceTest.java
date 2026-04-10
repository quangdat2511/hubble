package com.hubble.service;

import com.hubble.dto.response.AppLockSettingsResponse;
import com.hubble.entity.UserSettings;
import com.hubble.exception.AppException;
import com.hubble.exception.ErrorCode;
import com.hubble.repository.UserSettingsRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppLockSettingsServiceTest {

    @Mock
    private UserSettingsRepository userSettingsRepository;

    @InjectMocks
    private AppLockSettingsService appLockSettingsService;

    @Test
    void getAppLockSettings_WhenSettingsMissing_ReturnsNoPinConfigured() {
        UUID userId = UUID.randomUUID();
        when(userSettingsRepository.findById(userId)).thenReturn(Optional.empty());

        AppLockSettingsResponse response = appLockSettingsService.getAppLockSettings(userId);

        assertFalse(response.isPinConfigured());
        assertNull(response.getAppLockPin());
    }

    @Test
    void updateAppLockPin_WhenValidPin_SavesPin() {
        UUID userId = UUID.randomUUID();
        when(userSettingsRepository.findById(userId)).thenReturn(Optional.empty());
        when(userSettingsRepository.save(any(UserSettings.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AppLockSettingsResponse response = appLockSettingsService.updateAppLockPin(userId, "1234");

        verify(userSettingsRepository).save(any(UserSettings.class));
        assertTrue(response.isPinConfigured());
        assertEquals("1234", response.getAppLockPin());
    }

    @Test
    void updateAppLockPin_WhenCleared_RemovesPin() {
        UUID userId = UUID.randomUUID();
        UserSettings existingSettings = UserSettings.builder()
                .userId(userId)
                .appLockPin("1234")
                .build();
        when(userSettingsRepository.findById(userId)).thenReturn(Optional.of(existingSettings));
        when(userSettingsRepository.save(any(UserSettings.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AppLockSettingsResponse response = appLockSettingsService.updateAppLockPin(userId, " ");

        assertFalse(response.isPinConfigured());
        assertNull(response.getAppLockPin());
        assertNull(existingSettings.getAppLockPin());
        assertNotNull(existingSettings.getUpdatedAt());
    }

    @Test
    void updateAppLockPin_WhenInvalidPin_ThrowsBadRequest() {
        UUID userId = UUID.randomUUID();
        when(userSettingsRepository.findById(userId)).thenReturn(Optional.empty());

        AppException exception = assertThrows(AppException.class,
                () -> appLockSettingsService.updateAppLockPin(userId, "12ab"));

        assertEquals(ErrorCode.INVALID_APP_LOCK_PIN, exception.getErrorCode());
    }
}
