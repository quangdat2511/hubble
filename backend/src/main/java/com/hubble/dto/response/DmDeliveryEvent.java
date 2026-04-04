package com.hubble.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * Pushed to /topic/users/{recipientId}/dm-delivery when a DM message
 * is saved and broadcast by the server.  The recipient's device uses this
 * to immediately ack delivery — even when the user is not in the chat screen.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DmDeliveryEvent {
    String channelId;
    String senderId;
}
