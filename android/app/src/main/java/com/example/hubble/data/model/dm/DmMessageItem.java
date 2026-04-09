package com.example.hubble.data.model.dm;

import java.util.ArrayList;
import java.util.List;

public class DmMessageItem {

    public enum MessageStatus {
        SENDING,
        SENT,
        DELIVERED
    }

    private final String id;
    private final String senderName;
    private String content;
    private final String timestamp;
    private final String type;
    private final long createdAtMillis;
    private final boolean mine;

    private final List<AttachmentResponse> attachments;
    private List<ReactionDto> reactions;

    private boolean edited;
    private boolean deleted;
    private String replyToSenderName;
    private String replyToContent;
    private boolean replyToMine;
    private MessageStatus status;

    public DmMessageItem(String id, String senderName, String content, String timestamp, boolean mine) {
        this(id, senderName, content, timestamp, "TEXT", -1L, mine, null);
    }

    public DmMessageItem(String id, String senderName, String content,
                         String timestamp, boolean mine,
                         List<AttachmentResponse> attachments) {
        this(id, senderName, content, timestamp, "TEXT", -1L, mine, attachments);
    }

    public DmMessageItem(String id, String senderName, String content,
                         String timestamp, String type, long createdAtMillis,
                         boolean mine, List<AttachmentResponse> attachments) {
        this.id = id;
        this.senderName = senderName;
        this.content = content;
        this.timestamp = timestamp;
        this.type = type;
        this.createdAtMillis = createdAtMillis;
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

    // Của main
    public void setContent(String content) {
        this.content = content;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String getType() {
        return type;
    }

    public long getCreatedAtMillis() {
        return createdAtMillis;
    }

    public boolean isSystemMessage() {
        return type != null && "SYSTEM".equalsIgnoreCase(type);
    }

    public boolean isDateSeparator() {
        return "DATE_SEPARATOR".equals(type);
    }

    public static DmMessageItem createDateSeparator(String label) {
        return new DmMessageItem(null, null, label, null, "DATE_SEPARATOR", -1L, false, null);
    }

    public boolean isIntro() {
        return "INTRO".equals(type);
    }

    /**
     * Creates the profile intro item that appears as the first item in the chat.
     * senderName = displayName, timestamp = username, content = description
     */
    public static DmMessageItem createIntro(String displayName, String username, String description) {
        return new DmMessageItem("__intro__", displayName, description, username, "INTRO", -1L, false, null);
    }

    public boolean isMine() {
        return mine;
    }

    public List<AttachmentResponse> getAttachments() {
        return attachments;
    }


    public boolean isEdited() {
        return edited;
    }

    public void setEdited(boolean edited) {
        this.edited = edited;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public String getReplyToSenderName() {
        return replyToSenderName;
    }

    public void setReplyToSenderName(String replyToSenderName) {
        this.replyToSenderName = replyToSenderName;
    }

    public String getReplyToContent() {
        return replyToContent;
    }

    public void setReplyToContent(String replyToContent) {
        this.replyToContent = replyToContent;
    }

    public boolean isReplyToMine() {
        return replyToMine;
    }

    public void setReplyToMine(boolean replyToMine) {
        this.replyToMine = replyToMine;
    }

    public boolean hasReply() {
        return replyToSenderName != null && !replyToSenderName.trim().isEmpty();
    }

    public List<ReactionDto> getReactions() {
        return reactions != null ? reactions : new ArrayList<>();
    }

    public void setReactions(List<ReactionDto> reactions) {
        this.reactions = reactions;
    }

    public MessageStatus getStatus() {
        return status;
    }

    public void setStatus(MessageStatus status) {
        this.status = status;
    }
}