package com.hubble.dto.response;

import com.hubble.enums.PresenceStatus;
import com.hubble.enums.Theme;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserResponse {

    String id;
    String email;
    String tag;
    String displayName;
    String avatarUrl;
    String bio;
    String customStatus;
    PresenceStatus presenceStatus;
    LocalDateTime lastSeenAt;
    Theme theme;
    String language;
    LocalDateTime createdAt;
}
