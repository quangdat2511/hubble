package com.hubble.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class AttachmentResponse {
    private UUID id;
    private String filename;
    private String url;
    private String contentType;
    private Long sizeBytes;
}
