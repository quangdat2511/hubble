package com.hubble.controller;

import com.hubble.dto.common.ApiResponse;
import com.hubble.dto.request.PushConfigUpdateRequest;
import com.hubble.dto.response.PushConfigResponse;
import com.hubble.service.PushConfigService;
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
@RequestMapping("/api/settings/push")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PushConfigController {

    PushConfigService service;

    @GetMapping
    public ResponseEntity<ApiResponse<PushConfigResponse>> getConfig(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        PushConfigResponse response = service.getPushConfig(userId);
        return ResponseEntity.ok(ApiResponse.<PushConfigResponse>builder().result(response).build());
    }

    @PutMapping
    public ResponseEntity<ApiResponse<PushConfigResponse>> updateConfig(
            Authentication authentication,
            @RequestBody PushConfigUpdateRequest request
    ) {
        UUID userId = UUID.fromString(authentication.getName());
        PushConfigResponse response = service.updatePushConfig(userId, request);
        return ResponseEntity.ok(ApiResponse.<PushConfigResponse>builder().result(response).build());
    }
}
