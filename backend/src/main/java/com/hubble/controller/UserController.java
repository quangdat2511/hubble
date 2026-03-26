package com.hubble.controller;

import com.hubble.dto.common.ApiResponse;
import com.hubble.dto.request.UpdateCustomStatusRequest;
import com.hubble.dto.request.UpdateProfileRequest;
import com.hubble.dto.response.AvatarResponse;
import com.hubble.dto.response.UserResponse;
import com.hubble.service.UserService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
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

    @PutMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
            Authentication authentication,
            @RequestBody UpdateProfileRequest request
    ) {
        UUID userId = UUID.fromString(authentication.getName());
        UserResponse response = userService.updateProfile(userId, request);

        return ResponseEntity.ok(
                ApiResponse.<UserResponse>builder()
                        .result(response)
                        .build()
        );
    }

    @PutMapping("/me/custom-status")
    public ResponseEntity<ApiResponse<UserResponse>> updateCustomStatus(
            Authentication authentication,
            @RequestBody UpdateCustomStatusRequest request
    ) {
        UUID userId = UUID.fromString(authentication.getName());
        UserResponse response = userService.updateCustomStatus(userId, request);

        return ResponseEntity.ok(
                ApiResponse.<UserResponse>builder()
                        .result(response)
                        .build()
        );
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
