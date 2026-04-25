package com.hubble.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * Search result for a media or file attachment.
 * Includes the originating message and channel context for jump-to-message.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SearchAttachmentResponse {
    String id;
    String messageId;
    String channelId;
    String filename;
    String url;
    String contentType;
    Long sizeBytes;
    String createdAt;
}
