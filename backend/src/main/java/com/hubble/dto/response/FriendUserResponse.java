package com.hubble.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FriendUserResponse {
    UUID id;
    String username;
    String displayName;
    String avatarUrl;
    String status;
    String customStatus;
    String relationStatus;
}
