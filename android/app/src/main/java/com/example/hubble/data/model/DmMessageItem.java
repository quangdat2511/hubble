package com.example.hubble.data.model;

import java.util.ArrayList;
import java.util.List;

public class DmMessageItem {

    private final String id;
    private final String senderName;
    private final String content;
    private final String timestamp;
    private final boolean mine;
    private final List<AttachmentResponse> attachments;

    public DmMessageItem(String id, String senderName, String content, String timestamp, boolean mine) {
        this.id = id;
        this.senderName = senderName;
        this.content = content;
        this.timestamp = timestamp;
        this.mine = mine;
        this.attachments = new ArrayList<>();
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

    public String getTimestamp() {
        return timestamp;
    }

    public boolean isMine() {
        return mine;
    }

    public List<AttachmentResponse> getAttachments() { return attachments; }
}
