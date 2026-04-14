package com.example.hubble.data.model.voice;

public class VoiceParticipant {
    private String userId;
    private String displayName;
    private String avatarUrl;
    private boolean speaking;

    public VoiceParticipant() {}

    public VoiceParticipant(String userId, String displayName, String avatarUrl) {
        this.userId = userId;
        this.displayName = displayName;
        this.avatarUrl = avatarUrl;
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public boolean isSpeaking() { return speaking; }
    public void setSpeaking(boolean speaking) { this.speaking = speaking; }
}
