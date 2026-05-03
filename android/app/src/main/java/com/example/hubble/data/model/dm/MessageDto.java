package com.example.hubble.data.model.dm;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

public class MessageDto {
    private String id;
    private String channelId;
    private String authorId;
    @SerializedName("authorUsername")
    private String authorUsername;
    @SerializedName("authorDisplayName")
    private String authorDisplayName;
    private String replyToId;
    private String content;
    private String type;
    private Boolean isPinned;
    private Boolean isDeleted;
    private String editedAt;
    private String createdAt;

    @SerializedName("attachments")
    private List<AttachmentResponse> attachments;

    @SerializedName("reactions")
    private List<ReactionDto> reactions;

    @SerializedName("mentionedUserIds")
    private List<String> mentionedUserIds;

    @SerializedName("mentionedUsernames")
    private List<String> mentionedUsernames;

    public String getId() {
        return id;
    }

    public String getChannelId() {
        return channelId;
    }

    public String getAuthorId() {
        return authorId;
    }

    public String getAuthorUsername() {
        return authorUsername;
    }

    public String getAuthorDisplayName() {
        return authorDisplayName;
    }

    public String getReplyToId() {
        return replyToId;
    }

    public String getContent() {
        return content;
    }

    public String getType() {
        return type;
    }

    public Boolean getIsPinned() {
        return isPinned;
    }

    public Boolean getIsDeleted() {
        return isDeleted;
    }

    public String getEditedAt() {
        return editedAt;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public List<AttachmentResponse> getAttachments() {
        return attachments != null ? attachments : new ArrayList<>();
    }

    public List<ReactionDto> getReactions() {
        return reactions != null ? reactions : new ArrayList<>();
    }
    public void setReactions(List<ReactionDto> reactions) { this.reactions = reactions; }

    public List<String> getMentionedUserIds() {
        return mentionedUserIds != null ? mentionedUserIds : new ArrayList<>();
    }

    public List<String> getMentionedUsernames() {
        return mentionedUsernames != null ? mentionedUsernames : new ArrayList<>();
    }

    public void setId(String id) { this.id = id; }
    public void setChannelId(String channelId) { this.channelId = channelId; }
    public void setAuthorId(String authorId) { this.authorId = authorId; }
    public void setReplyToId(String replyToId) { this.replyToId = replyToId; }
    public void setContent(String content) { this.content = content; }
    public void setType(String type) { this.type = type; }
    public void setIsPinned(Boolean isPinned) { this.isPinned = isPinned; }
    public void setIsDeleted(Boolean isDeleted) { this.isDeleted = isDeleted; }
    public void setEditedAt(String editedAt) { this.editedAt = editedAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
