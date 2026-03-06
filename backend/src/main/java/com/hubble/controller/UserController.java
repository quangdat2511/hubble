package com.hubble.controller;

import com.hubble.dto.request.CreateUserRequest;
import com.hubble.dto.response.UserResponse;
import com.hubble.dto.common.ApiResponse;
import com.hubble.service.UserService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserController {

    UserService userService;
    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    @PostMapping("/sync")
    public ResponseEntity<ApiResponse<UserResponse>> syncUser(@Valid @RequestBody CreateUserRequest request) {
        UserResponse userResponse = userService.syncFirebaseUser(request);

        log.info("API /users/sync was called with email: {}", request.getEmail());

        ApiResponse<UserResponse> response = ApiResponse.<UserResponse>builder()
                .result(userResponse)
                .build();

        return ResponseEntity.ok(response);
    }
}