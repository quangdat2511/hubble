package com.hubble.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RoleDetailResponse {
    UUID id;
    String name;
    Integer color;
    Long permissions;
    Boolean displaySeparately;
    Boolean mentionable;
    Integer memberCount;
    List<PermissionResponse> permissionDetails;
    List<MemberBriefResponse> members;
}
