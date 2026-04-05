package com.example.hubble.data.model.notify;

public class NotificationResponse {
    private String id;
    private String type;
    private String referenceId;
    private String content;
    private Boolean isRead;
    private String createdAt;

    public String getId() { return id; }
    public String getType() { return type; }
    public String getReferenceId() { return referenceId; }
    public String getContent() { return content; }
    public Boolean getIsRead() { return isRead; }
    public String getCreatedAt() { return createdAt; }
}
