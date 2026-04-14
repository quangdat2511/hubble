package com.example.hubble.data.model.voice;

public class VoiceSignalMessage {
    private String type;       // "join", "leave", "offer", "answer", "ice-candidate"
    private String channelId;
    private String userId;
    private String displayName;
    private String avatarUrl;
    private String targetUserId;
    private String sdp;
    private String candidate;
    private String sdpMid;
    private Integer sdpMLineIndex;

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getChannelId() { return channelId; }
    public void setChannelId(String channelId) { this.channelId = channelId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public String getTargetUserId() { return targetUserId; }
    public void setTargetUserId(String targetUserId) { this.targetUserId = targetUserId; }

    public String getSdp() { return sdp; }
    public void setSdp(String sdp) { this.sdp = sdp; }

    public String getCandidate() { return candidate; }
    public void setCandidate(String candidate) { this.candidate = candidate; }

    public String getSdpMid() { return sdpMid; }
    public void setSdpMid(String sdpMid) { this.sdpMid = sdpMid; }

    public Integer getSdpMLineIndex() { return sdpMLineIndex; }
    public void setSdpMLineIndex(Integer sdpMLineIndex) { this.sdpMLineIndex = sdpMLineIndex; }
}
