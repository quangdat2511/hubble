package com.hubble.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ChannelResponse {
    String id;
    String serverId;
    String parentId;
    String name;
    String type;
    String topic;
    Short position;
    Boolean isPrivate;
    Boolean canAccess;
    String createdAt;
    String peerUserId;
    String peerUsername;
    String peerDisplayName;
    String peerAvatarUrl;
    String peerStatus;
    /** Incoming messages (from others) after the user's last_read_at; null if not computed */
    Integer unreadCount;
}
