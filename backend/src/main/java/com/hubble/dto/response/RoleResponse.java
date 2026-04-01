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
public class RoleResponse {
    UUID id;
    UUID serverId;
    String name;
    Integer color;
    Long permissions;
    Integer position;
    Boolean isDefault;
    Boolean displaySeparately;
    Boolean mentionable;
    Integer memberCount;
    LocalDateTime createdAt;
}
