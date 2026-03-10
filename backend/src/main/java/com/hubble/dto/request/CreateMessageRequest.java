package com.hubble.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreateMessageRequest {
    UUID channelId;
    UUID authorId;
    UUID replyToId;
    String content;
    private String type;                  // TEXT | IMAGE | FILE | VOICE_NOTE
    private List<UUID> attachmentIds;
}
