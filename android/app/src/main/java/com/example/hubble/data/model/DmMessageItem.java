package com.example.hubble.data.model;

public class DmMessageItem {

    private final String id;
    private final String senderName;
    private final String content;
    private final String timestamp;
    private final boolean mine;

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

    public String getTimestamp() {
        return timestamp;
    }

    public boolean isMine() {
        return mine;
    }
}
