package com.hubble.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
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
    @JsonProperty("isSelf")
    boolean isSelf;
    @JsonProperty("isFriend")
    boolean isFriend;
    @JsonProperty("friendshipState")
    String friendshipState;
}
