package com.hubble.controller;

import com.hubble.dto.common.ApiResponse;
import com.hubble.dto.request.AppLockSettingsRequest;
import com.hubble.dto.response.AppLockSettingsResponse;
import com.hubble.service.AppLockSettingsService;
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
@RequestMapping("/api/settings/app-lock")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AppLockSettingsController {

    AppLockSettingsService service;

    @GetMapping
    public ResponseEntity<ApiResponse<AppLockSettingsResponse>> getSettings(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        return ResponseEntity.ok(ApiResponse.<AppLockSettingsResponse>builder()
                .result(service.getAppLockSettings(userId))
                .build());
    }

    @PutMapping
    public ResponseEntity<ApiResponse<AppLockSettingsResponse>> updateSettings(
            Authentication authentication,
            @RequestBody AppLockSettingsRequest request
    ) {
        UUID userId = UUID.fromString(authentication.getName());
        return ResponseEntity.ok(ApiResponse.<AppLockSettingsResponse>builder()
                .result(service.updateAppLockPin(userId, request != null ? request.getAppLockPin() : null))
                .build());
    }
}
