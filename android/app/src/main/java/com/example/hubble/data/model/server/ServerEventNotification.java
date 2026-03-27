package com.example.hubble.data.model.server;

public class ServerEventNotification {
    private final String type;
    private final String serverId;
    private final String serverName;

    public ServerEventNotification(String type, String serverId, String serverName) {
        this.type = type;
        this.serverId = serverId;
        this.serverName = serverName;
    }

    public String getType()       { return type; }
    public String getServerId()   { return serverId; }
    public String getServerName() { return serverName; }
}

