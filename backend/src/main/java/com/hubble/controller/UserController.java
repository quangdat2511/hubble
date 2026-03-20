package com.hubble.controller;

import com.hubble.dto.common.ApiResponse;
import com.hubble.dto.response.UserResponse;
import com.hubble.service.UserService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserController {

    UserService userService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getMyProfile(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        UserResponse userResponse = userService.getUserById(userId);
        return ResponseEntity.ok(ApiResponse.<UserResponse>builder().result(userResponse).build());
    }

    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable UUID userId) {
        UserResponse userResponse = userService.getUserById(userId);
        return ResponseEntity.ok(ApiResponse.<UserResponse>builder().result(userResponse).build());
    }

    //GET QR
    @GetMapping("/me/qr")
    public ResponseEntity<ApiResponse<String>> getMyQr(Authentication auth) {
        UUID userId = UUID.fromString(auth.getName());

        String token = userService.generateQrToken(userId);

        return ResponseEntity.ok(ApiResponse.<String>builder()
                .result(token)
                .build());
    }

    // SCAN QR
    @GetMapping("/scan/qr")
    public ResponseEntity<ApiResponse<UserResponse>> scan(@RequestParam String token) {
        return ResponseEntity.ok(ApiResponse.<UserResponse>builder()
                .result(userService.getUserFromQr(token))
                .build());
    }
}