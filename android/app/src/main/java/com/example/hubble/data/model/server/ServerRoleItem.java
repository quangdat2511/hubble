package com.example.hubble.data.model.server;

public class ServerRoleItem {
    private String id;
    private String name;
    private int color;

    public ServerRoleItem(String id, String name, int color) {
        this.id = id;
        this.name = name;
        this.color = color;
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
}
