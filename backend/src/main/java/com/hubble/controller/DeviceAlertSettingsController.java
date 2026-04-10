package com.hubble.controller;

import com.hubble.dto.common.ApiResponse;
import com.hubble.dto.request.DeviceAlertSettingsUpdateRequest;
import com.hubble.dto.response.DeviceAlertSettingsResponse;
import com.hubble.service.DeviceAlertSettingsService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/settings/security/device-alerts")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DeviceAlertSettingsController {

    DeviceAlertSettingsService service;

    @GetMapping
    public ResponseEntity<ApiResponse<DeviceAlertSettingsResponse>> getSettings(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        DeviceAlertSettingsResponse response = service.getSettings(userId);
        return ResponseEntity.ok(ApiResponse.<DeviceAlertSettingsResponse>builder().result(response).build());
    }

    @PutMapping
    public ResponseEntity<ApiResponse<DeviceAlertSettingsResponse>> updateSettings(
            Authentication authentication,
            @RequestBody DeviceAlertSettingsUpdateRequest request
    ) {
        UUID userId = UUID.fromString(authentication.getName());
        DeviceAlertSettingsResponse response = service.updateSettings(userId, request);
        return ResponseEntity.ok(ApiResponse.<DeviceAlertSettingsResponse>builder().result(response).build());
    }
}
