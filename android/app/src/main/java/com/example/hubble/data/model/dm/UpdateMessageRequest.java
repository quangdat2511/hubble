package com.example.hubble.data.model.dm;

public class UpdateMessageRequest {
    private final String content;

    public UpdateMessageRequest(String content) {
        this.content = content;
    }

    public String getContent() {
        return content;
    }
}
