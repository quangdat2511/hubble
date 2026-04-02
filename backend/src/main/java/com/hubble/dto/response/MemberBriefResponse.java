package com.hubble.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MemberBriefResponse {
    UUID serverMemberId;
    UUID userId;
    String displayName;
    String username;
    String avatarUrl;
}
