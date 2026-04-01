package com.hubble.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class UploadResponse {
    private UUID attachmentId;
    private String url;
    private String filename;
    private String contentType;
    private Long sizeBytes;
}

