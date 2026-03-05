package com.hubble.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MessageRequest {
    UUID channelId;
    UUID authorId;
    UUID replyToId;
    String content;
}
