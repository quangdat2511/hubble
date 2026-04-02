package com.example.hubble.data.model.server;

public class ServerInviteResponse {
    private String id;
    private String serverId;
    private String serverName;
    private String serverIconUrl;
    private String inviterId;
    private String inviterUsername;
    private String inviterDisplayName;
    private String inviteeId;
    private String inviteeUsername;
    private String inviteeDisplayName;
    private String status; // PENDING, ACCEPTED, DECLINED, EXPIRED
    private String createdAt;
    private String expiresAt;
    private String respondedAt;

    public ServerInviteResponse() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getServerId() { return serverId; }
    public void setServerId(String serverId) { this.serverId = serverId; }

    public String getServerName() { return serverName; }
    public void setServerName(String serverName) { this.serverName = serverName; }

    public String getServerIconUrl() { return serverIconUrl; }
    public void setServerIconUrl(String serverIconUrl) { this.serverIconUrl = serverIconUrl; }

    public String getInviterId() { return inviterId; }
    public void setInviterId(String inviterId) { this.inviterId = inviterId; }

    public String getInviterUsername() { return inviterUsername; }
    public void setInviterUsername(String inviterUsername) { this.inviterUsername = inviterUsername; }

    public String getInviterDisplayName() { return inviterDisplayName; }
    public void setInviterDisplayName(String inviterDisplayName) { this.inviterDisplayName = inviterDisplayName; }

    public String getInviteeId() { return inviteeId; }
    public void setInviteeId(String inviteeId) { this.inviteeId = inviteeId; }

    public String getInviteeUsername() { return inviteeUsername; }
    public void setInviteeUsername(String inviteeUsername) { this.inviteeUsername = inviteeUsername; }

    public String getInviteeDisplayName() { return inviteeDisplayName; }
    public void setInviteeDisplayName(String inviteeDisplayName) { this.inviteeDisplayName = inviteeDisplayName; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getExpiresAt() { return expiresAt; }
    public void setExpiresAt(String expiresAt) { this.expiresAt = expiresAt; }

    public String getRespondedAt() { return respondedAt; }
    public void setRespondedAt(String respondedAt) { this.respondedAt = respondedAt; }
}

