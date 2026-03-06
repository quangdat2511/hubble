package com.hubble.dto.response;

import com.hubble.enums.MessageType;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MessageResponse {

    UUID id;
    UUID channelId;
    UUID authorId;
    UUID replyToId;
    String content;
    MessageType type;
    Boolean isPinned;
    LocalDateTime editedAt;
    LocalDateTime createdAt;
}
