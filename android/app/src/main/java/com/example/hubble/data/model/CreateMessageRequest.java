package com.example.hubble.data.model;

import java.util.ArrayList;
import java.util.List;

public class CreateMessageRequest {
    private final String channelId;
    private final String replyToId;
    private final String content;

    private final String type;
    private final List<String> attachmentIds;

    public CreateMessageRequest(String channelId, String replyToId, String content) {
        this.channelId = channelId;
        this.replyToId = replyToId;
        this.content = content;
        this.type = "TEXT";
        this.attachmentIds = new ArrayList<>();
    }

    public CreateMessageRequest(String channelId, String replyToId, String content,
                                String type, List<String> attachmentIds) {
        this.channelId = channelId;
        this.replyToId = replyToId;
        this.content = content;
        this.type = type;
        this.attachmentIds = attachmentIds != null ? attachmentIds : new ArrayList<>();
    }

    public String getChannelId() {
        return channelId;
    }

    public String getReplyToId() {
        return replyToId;
    }

    public String getContent() {
        return content;
    }

    public String getType() { return type; }

    public List<String> getAttachmentIds() { return attachmentIds; }
}
