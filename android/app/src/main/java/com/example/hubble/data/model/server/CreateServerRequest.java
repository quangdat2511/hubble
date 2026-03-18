package com.example.hubble.data.model.server;

public class CreateServerRequest {

    private final String name;
    private final String type;

    public CreateServerRequest(String name, String type) {
        this.name = name;
        this.type = type;
    }

    public String getName() { return name; }
    public String getType() { return type; }
}

