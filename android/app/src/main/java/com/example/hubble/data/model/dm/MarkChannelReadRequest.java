package com.example.hubble.data.model.dm;

public class MarkChannelReadRequest {
    private final String messageId;

    public MarkChannelReadRequest(String messageId) {
        this.messageId = messageId;
    }

    public String getMessageId() {
        return messageId;
    }
}
