package com.hubble.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MessageResponse {

    String id;
    String channelId;
    String authorId;
    /** Denormalized for clients (e.g. server text channels) — may be null for old rows. */
    String authorUsername;
    String authorDisplayName;
    String replyToId;
    String content;
    String type;
    Boolean isPinned;

    Boolean isDeleted;

    String editedAt;
    String createdAt;

    List<AttachmentResponse> attachments;

    List<ReactionResponse> reactions;
}