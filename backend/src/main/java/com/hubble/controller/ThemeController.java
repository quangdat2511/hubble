package com.hubble.controller;

import com.hubble.dto.common.ApiResponse;
import com.hubble.service.ThemeService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/settings/theme")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ThemeController {

    ThemeService service;

    @GetMapping
    public ResponseEntity<ApiResponse<String>> getTheme(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        return ResponseEntity.ok(ApiResponse.<String>builder()
                .result(service.getTheme(userId))
                .build());
    }

    @PutMapping
    public ResponseEntity<ApiResponse<String>> updateTheme(
            Authentication authentication,
            @RequestParam String theme
    ) {
        UUID userId = UUID.fromString(authentication.getName());
        String updatedTheme = service.updateTheme(userId, theme);
        return ResponseEntity.ok(ApiResponse.<String>builder()
                .result(updatedTheme)
                .build());
    }
}
