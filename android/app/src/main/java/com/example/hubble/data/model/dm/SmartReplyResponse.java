package com.example.hubble.data.model.dm;

import java.util.List;

public class SmartReplyResponse {
    private List<String> suggestions;
    private String messageAuthorId;

    public SmartReplyResponse() {}

    public List<String> getSuggestions() { return suggestions; }
    public void setSuggestions(List<String> suggestions) { this.suggestions = suggestions; }

    public String getMessageAuthorId() { return messageAuthorId; }
    public void setMessageAuthorId(String messageAuthorId) { this.messageAuthorId = messageAuthorId; }
}