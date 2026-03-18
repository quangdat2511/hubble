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
public class FriendRequestResponse {
    UUID id;
    UUID requesterId;
    UUID addresseeId;
    String status;
    LocalDateTime createdAt;
    boolean incoming;
    FriendUserResponse user;
}
