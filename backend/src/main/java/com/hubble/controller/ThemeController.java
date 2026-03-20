package com.hubble.controller;

import com.hubble.service.ThemeService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/settings/theme")
public class ThemeController {

    private final ThemeService service;

    public ThemeController(ThemeService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<String> getTheme(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        return ResponseEntity.ok(service.getTheme(userId));
    }

    @PutMapping
    public ResponseEntity<Void> updateTheme(
            Authentication authentication,
            @RequestParam String theme
    ) {
        UUID userId = UUID.fromString(authentication.getName());
        service.updateTheme(userId, theme);
        return ResponseEntity.ok().build();
    }
}