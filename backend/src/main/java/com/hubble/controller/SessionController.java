package com.hubble.controller;

import com.hubble.dto.common.ApiResponse;
import com.hubble.dto.response.SessionResponse;
import com.hubble.security.UserPrincipal;
import com.hubble.service.SessionService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SessionController {

    SessionService sessionService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<SessionResponse>>> getActiveSessions(
            @AuthenticationPrincipal UserPrincipal principal) {
        List<SessionResponse> sessions = sessionService.getActiveSessions(principal.getId());
        return ResponseEntity.ok(ApiResponse.<List<SessionResponse>>builder().result(sessions).build());
    }

    @DeleteMapping("/{sessionId}")
    public ResponseEntity<ApiResponse<String>> revokeSession(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID sessionId) {
        sessionService.revokeSession(principal.getId(), sessionId);
        return ResponseEntity.ok(ApiResponse.<String>builder().result("Đã đăng xuất thiết bị thành công").build());
    }
}