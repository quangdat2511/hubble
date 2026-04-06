package com.hubble.controller;

import com.hubble.dto.common.ApiResponse;
import com.hubble.entity.DeviceToken;
import com.hubble.repository.DeviceTokenRepository;
import com.hubble.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/device-tokens")
@RequiredArgsConstructor
public class DeviceTokenController {

    private final DeviceTokenRepository deviceTokenRepository;

    @PostMapping
    @Transactional
    public ApiResponse<Void> registerToken(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody Map<String, String> request) {
        String token = request.get("token");
        String deviceType = request.get("deviceType");

        if (!deviceTokenRepository.existsByToken(token)) {
            deviceTokenRepository.save(DeviceToken.builder()
                    .userId(principal.getId())
                    .token(token)
                    .deviceType(deviceType)
                    .build());
        }
        return ApiResponse.<Void>builder().build();
    }

    @DeleteMapping("/{token}")
    @Transactional
    public ApiResponse<Void> removeToken(@PathVariable String token) {
        deviceTokenRepository.deleteByToken(token);
        return ApiResponse.<Void>builder().build();
    }
}