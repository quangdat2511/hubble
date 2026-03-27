package com.hubble.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ServerEventNotification {
    String type;      // e.g. "KICKED", "SERVER_DELETED", "ROLE_UPDATED"
    UUID serverId;
    String serverName;
}

