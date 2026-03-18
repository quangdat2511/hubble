package com.example.hubble.data.model.dm;

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
}

