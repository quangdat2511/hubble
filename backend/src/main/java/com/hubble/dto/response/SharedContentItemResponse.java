package com.hubble.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SharedContentItemResponse {
    String id;
    String messageId;
    String type;
    String url;
    String previewUrl;
    String filename;
    String contentType;
    Long sizeBytes;
    String messageContent;
    String createdAt;
}
