package com.example.hubble.data.model.server;

/** DTO that mirrors what the backend returns for a server. */
public class ServerResponse {
    private String id;
    private String name;
    private String iconUrl;

    public ServerResponse() {}

    public String getId() { return id; }
    public String getName() { return name; }
    public String getIconUrl() { return iconUrl; }
}

