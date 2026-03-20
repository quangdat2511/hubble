package com.hubble.controller;

import com.hubble.entity.UserSettings;
import com.hubble.service.PushConfigService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/settings/push")
public class PushConfigController {

    private final PushConfigService service;

    public PushConfigController(PushConfigService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<UserSettings> getConfig(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());

        return ResponseEntity.ok(service.getPushConfig(userId));
    }

    @PutMapping
    public ResponseEntity<Void> updateConfig(
            Authentication authentication,
            @RequestParam Boolean enabled,
            @RequestParam Boolean sound
    ) {

        UUID userId = UUID.fromString(authentication.getName());
        service.updatePushConfig(userId, enabled, sound);
        return ResponseEntity.ok().build();
    }
}
