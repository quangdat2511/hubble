package com.example.hubble.data.model;

public class MessageDto {
    private String id;
    private String channelId;
    private String authorId;
    private String replyToId;
    private String content;
    private String type;
    private Boolean isPinned;
    private String editedAt;
    private String createdAt;

    public String getId() {
        return id;
    }

    public String getChannelId() {
        return channelId;
    }

    public String getAuthorId() {
        return authorId;
    }

    public String getReplyToId() {
        return replyToId;
    }

    public String getContent() {
        return content;
    }

    public String getType() {
        return type;
    }

    public Boolean getIsPinned() {
        return isPinned;
    }

    public String getEditedAt() {
        return editedAt;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setId(String id) { this.id = id; }
    public void setChannelId(String channelId) { this.channelId = channelId; }
    public void setAuthorId(String authorId) { this.authorId = authorId; }
    public void setReplyToId(String replyToId) { this.replyToId = replyToId; }
    public void setContent(String content) { this.content = content; }
    public void setType(String type) { this.type = type; }
    public void setIsPinned(Boolean isPinned) { this.isPinned = isPinned; }
    public void setEditedAt(String editedAt) { this.editedAt = editedAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
