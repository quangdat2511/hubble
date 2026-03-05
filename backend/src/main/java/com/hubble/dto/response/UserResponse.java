package com.hubble.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserResponse {
    UUID id;
    String username;
    String displayName;
    String email;
    String avatarUrl;
    String bio;
    String status;
    String phone;
    LocalDateTime createdAt;
}