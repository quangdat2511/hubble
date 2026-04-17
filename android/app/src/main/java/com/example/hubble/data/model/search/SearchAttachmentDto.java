package com.example.hubble.data.model.search;

public class SearchAttachmentDto {
    private String id;
    private String messageId;
    private String channelId;
    private String filename;
    private String url;
    private String contentType;
    private Long sizeBytes;
    private String createdAt;

    public String getId() { return id; }
    public String getMessageId() { return messageId; }
    public String getChannelId() { return channelId; }
    public String getFilename() { return filename; }
    public String getUrl() { return url; }
    public String getContentType() { return contentType; }
    public Long getSizeBytes() { return sizeBytes; }
    public String getCreatedAt() { return createdAt; }
}
