package com.hubble.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

/** Search result for a user (server member or DM friend). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SearchMemberResponse {
    String id;
    String username;
    String displayName;
    String avatarUrl;
    String status;
}
