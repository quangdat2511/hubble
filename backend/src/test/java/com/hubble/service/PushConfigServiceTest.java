package com.hubble.service;

import com.hubble.dto.request.PushConfigUpdateRequest;
import com.hubble.dto.response.PushConfigResponse;
import com.hubble.entity.UserSettings;
import com.hubble.repository.UserSettingsRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PushConfigServiceTest {

    @Mock
    private UserSettingsRepository userSettingsRepository;

    @InjectMocks
    private PushConfigService pushConfigService;

    @Test
    void getPushConfig_WhenSettingsMissing_ReturnsDefaults() {
        UUID userId = UUID.randomUUID();
        when(userSettingsRepository.findById(userId)).thenReturn(Optional.empty());

        PushConfigResponse response = pushConfigService.getPushConfig(userId);

        assertTrue(response.isNotificationEnabled());
        assertTrue(response.isNotificationSound());
    }

    @Test
    void updatePushConfig_WhenSettingsMissing_CreatesDefaultsAndSavesRequestValues() {
        UUID userId = UUID.randomUUID();
        PushConfigUpdateRequest request = PushConfigUpdateRequest.builder()
                .notificationEnabled(false)
                .notificationSound(true)
                .build();

        when(userSettingsRepository.findById(userId)).thenReturn(Optional.empty());
        when(userSettingsRepository.save(any(UserSettings.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PushConfigResponse response = pushConfigService.updatePushConfig(userId, request);

        verify(userSettingsRepository).save(any(UserSettings.class));
        assertFalse(response.isNotificationEnabled());
        assertTrue(response.isNotificationSound());
    }

    @Test
    void updatePushConfig_WhenSettingsExist_UpdatesTimestampAndFlags() {
        UUID userId = UUID.randomUUID();
        UserSettings existingSettings = UserSettings.builder()
                .userId(userId)
                .theme("DARK")
                .locale("vi")
                .notificationEnabled(true)
                .notificationSound(true)
                .build();

        when(userSettingsRepository.findById(userId)).thenReturn(Optional.of(existingSettings));
        when(userSettingsRepository.save(any(UserSettings.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PushConfigResponse response = pushConfigService.updatePushConfig(userId, PushConfigUpdateRequest.builder()
                .notificationEnabled(false)
                .notificationSound(false)
                .build());

        assertFalse(response.isNotificationEnabled());
        assertFalse(response.isNotificationSound());
        assertNotNull(existingSettings.getUpdatedAt());
    }
}
