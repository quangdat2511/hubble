package com.example.hubble.data.model.server;

public class ServerEventNotification {
    private final String type;
    private final String serverId;
    private final String serverName;
    private final String roleId;

    public ServerEventNotification(String type, String serverId, String serverName) {
        this(type, serverId, serverName, null);
    }

    public ServerEventNotification(String type, String serverId, String serverName, String roleId) {
        this.type = type;
        this.serverId = serverId;
        this.serverName = serverName;
        this.roleId = roleId;
    }

    public String getType()       { return type; }
    public String getServerId()   { return serverId; }
    public String getServerName() { return serverName; }
    public String getRoleId()     { return roleId; }
}

