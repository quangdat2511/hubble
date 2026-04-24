package com.example.hubble.data.model.search;

import com.google.gson.annotations.SerializedName;
import com.example.hubble.data.model.dm.AttachmentResponse;

import java.util.List;

public class SearchMessageDto {
    private String id;
    private String channelId;
    private String channelName;
    private String authorId;
    private String authorUsername;
    private String authorDisplayName;
    private String authorAvatarUrl;
    private String content;
    private String type;
    @SerializedName("isPinned")
    private Boolean isPinned;
    private String createdAt;
    private List<AttachmentResponse> attachments;

    public String getId() { return id; }
    public String getChannelId() { return channelId; }
    public String getChannelName() { return channelName; }
    public String getAuthorId() { return authorId; }
    public String getAuthorUsername() { return authorUsername; }
    public String getAuthorDisplayName() { return authorDisplayName; }
    public String getAuthorAvatarUrl() { return authorAvatarUrl; }
    public String getContent() { return content; }
    public String getType() { return type; }
    public Boolean getIsPinned() { return isPinned; }
    public String getCreatedAt() { return createdAt; }
    public List<AttachmentResponse> getAttachments() { return attachments; }
}
