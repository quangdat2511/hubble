package com.hubble.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AttachmentResponse {
    UUID id;
    UUID messageId;
    String filename;
    String url;
    String contentType;
    Long sizeBytes;
    LocalDateTime createdAt;
}
