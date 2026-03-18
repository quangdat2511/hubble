package com.example.hubble.data.model;

public class CreateMessageRequest {
    private final String channelId;
    private final String replyToId;
    private final String content;

    public CreateMessageRequest(String channelId, String replyToId, String content) {
        this.channelId = channelId;
        this.replyToId = replyToId;
        this.content = content;
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
}
