package com.hubble.controller;

import com.hubble.entity.UserSettings;
import com.hubble.service.LanguageService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/settings/language")
public class LanguageController {

    private final LanguageService service;

    public LanguageController(LanguageService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<String> getLanguage(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());

        return ResponseEntity.ok(service.getLanguage(userId));
    }

    @PutMapping
    public ResponseEntity<Void> updateLanguage(
            Authentication authentication,
            @RequestParam String locale
    ) {
        UUID userId = UUID.fromString(authentication.getName());
        service.updateLanguage(userId, locale);
        return ResponseEntity.ok().build();
    }
}
