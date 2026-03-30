package com.example.hubble.data.model.dm;

import java.util.ArrayList;
import java.util.List;

public class DmMessageItem {

    private final String id;
    private final String senderName;
    private String content;
    private final String timestamp;
    private final boolean mine;

    private final List<AttachmentResponse> attachments;

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
        this.attachments = new ArrayList<>(); // Code của bạn
    }

    public DmMessageItem(String id, String senderName, String content,
                         String timestamp, boolean mine,
                         List<AttachmentResponse> attachments) {
        this.id = id;
        this.senderName = senderName;
        this.content = content;
        this.timestamp = timestamp;
        this.mine = mine;
        this.attachments = attachments != null ? attachments : new ArrayList<>();
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

    // Của main
    public void setContent(String content) {
        this.content = content;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public boolean isMine() {
        return mine;
    }

    public List<AttachmentResponse> getAttachments() {
        return attachments;
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