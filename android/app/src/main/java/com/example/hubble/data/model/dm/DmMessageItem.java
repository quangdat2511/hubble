package com.example.hubble.data.model.dm;

public class DmMessageItem {

    private final String id;
    private final String senderName;
    private String content;
    private final String timestamp;
    private final boolean mine;
    private boolean edited;
    private boolean deleted;
    private String replyToSenderName;
    private String replyToContent;

    public DmMessageItem(String id, String senderName, String content, String timestamp, boolean mine) {
        this.id = id;
        this.senderName = senderName;
        this.content = content;
        this.timestamp = timestamp;
        this.mine = mine;
    }

    public String getId() {
        return id;
    }

    public String getSenderName() {
        return senderName;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public boolean isMine() {
        return mine;
    }

    public boolean isEdited() {
        return edited;
    }

    public void setEdited(boolean edited) {
        this.edited = edited;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public String getReplyToSenderName() {
        return replyToSenderName;
    }

    public void setReplyToSenderName(String replyToSenderName) {
        this.replyToSenderName = replyToSenderName;
    }

    public String getReplyToContent() {
        return replyToContent;
    }

    public void setReplyToContent(String replyToContent) {
        this.replyToContent = replyToContent;
    }

    public boolean hasReply() {
        return replyToSenderName != null && !replyToSenderName.trim().isEmpty();
    }
}

