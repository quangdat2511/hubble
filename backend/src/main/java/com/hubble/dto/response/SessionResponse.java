package com.hubble.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SessionResponse {
    UUID id;
    String deviceName;
    String deviceType;
    String ipAddress;
    LocalDateTime lastActiveAt;
    LocalDateTime createdAt;
}