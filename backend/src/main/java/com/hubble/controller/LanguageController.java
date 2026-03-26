package com.hubble.controller;

import com.hubble.dto.common.ApiResponse;
import com.hubble.service.LanguageService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/settings/language")
public class LanguageController {

    private final LanguageService service;

    public LanguageController(LanguageService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<String>> getLanguage(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        String locale = service.getLanguage(userId);

        return ResponseEntity.ok(ApiResponse.<String>builder()
                .result(locale)
                .build());
    }

    @PutMapping
    public ResponseEntity<ApiResponse<String>> updateLanguage(
            Authentication authentication,
            @RequestParam String locale
    ) {
        UUID userId = UUID.fromString(authentication.getName());
        service.updateLanguage(userId, locale);

        return ResponseEntity.ok(ApiResponse.<String>builder()
                .message("Language updated successfully")
                .result(service.getLanguage(userId))
                .build());
    }
}
