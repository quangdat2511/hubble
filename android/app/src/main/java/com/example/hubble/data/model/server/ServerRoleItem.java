package com.example.hubble.data.model.server;

public class ServerRoleItem {
    private String id;
    private String name;
    private int color;
    private int memberCount;

    public ServerRoleItem(String id, String name, int color, int memberCount) {
        this.id = id;
        this.name = name;
        this.color = color;
        this.memberCount = memberCount;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getColor() {
        return color;
    }

    public int getMemberCount() {
        return memberCount;
    }
}
