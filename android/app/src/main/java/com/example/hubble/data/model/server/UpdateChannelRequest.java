package com.example.hubble.data.model.server;

public class UpdateChannelRequest {
    private String name;
    private String topic;
    private String parentId;
    private Boolean isPrivate;

    public UpdateChannelRequest() {}

    public UpdateChannelRequest(String name, String topic, String parentId, Boolean isPrivate) {
        this.name = name;
        this.topic = topic;
        this.parentId = parentId;
        this.isPrivate = isPrivate;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }

    public String getParentId() { return parentId; }
    public void setParentId(String parentId) { this.parentId = parentId; }

    public Boolean getIsPrivate() { return isPrivate; }
    public void setIsPrivate(Boolean isPrivate) { this.isPrivate = isPrivate; }
}
