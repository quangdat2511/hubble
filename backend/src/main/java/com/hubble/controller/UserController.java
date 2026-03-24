package com.hubble.controller;

import com.hubble.dto.common.ApiResponse;
import com.hubble.dto.response.AvatarResponse;
import com.hubble.dto.response.UserResponse;
import com.hubble.service.UserService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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

    @PostMapping(value = "/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<UserResponse>> updateAvatar(
            Authentication authentication,
            @RequestParam("file") MultipartFile file
    ) throws IOException {
        UUID userId = UUID.fromString(authentication.getName());
        UserResponse response = userService.updateAvatar(userId, file);

        return ResponseEntity.ok(
                ApiResponse.<UserResponse>builder()
                        .result(response)
                        .build()
        );
    }

    @GetMapping("/me/avatar")
    public ResponseEntity<ApiResponse<AvatarResponse>> getMyAvatar(Authentication authentication) throws IOException {
        UUID userId = UUID.fromString(authentication.getName());
        AvatarResponse avatarResponse = userService.getAvatarResponse(userId);

        return ResponseEntity.ok(
                ApiResponse.<AvatarResponse>builder()
                        .result(avatarResponse)
                        .build()
        );
    }

    @GetMapping("/{userId}/avatar")
    public ResponseEntity<ApiResponse<AvatarResponse>> getUserAvatar(@PathVariable UUID userId) throws IOException {
        AvatarResponse avatarResponse = userService.getAvatarResponse(userId);

        return ResponseEntity.ok(
                ApiResponse.<AvatarResponse>builder()
                        .result(avatarResponse)
                        .build()
        );
    }
}