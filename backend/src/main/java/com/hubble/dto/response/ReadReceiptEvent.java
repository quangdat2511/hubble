package com.hubble.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * Broadcast to /topic/channels/{channelId}/read when a member marks messages read.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReadReceiptEvent {
    String userId;
    /** Message up to which the reader has read (inclusive), for debugging / future use */
    String lastReadMessageId;
    /** ISO-8601 local time of that message's created_at — sender compares createdAt */
    String readAt;
}
