package com.hubble.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

/**
 * Search result for a message. Extends the standard message fields with
 * channel context (useful for server-scope search where results span channels).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SearchMessageResponse {
    String id;
    String channelId;
    String channelName;
    String authorId;
    String authorUsername;
    String authorDisplayName;
    String authorAvatarUrl;
    String content;
    String type;
    Boolean isPinned;
    String createdAt;
    List<AttachmentResponse> attachments;
}
