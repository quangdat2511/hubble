package com.example.hubble.data.model.server;

/**
 * Maps to backend PermissionResponse DTO.
 */
public class PermissionResponse {
    private String name;
    private String description;
    private Boolean granted;

    public String getName() { return name; }
    public String getDescription() { return description; }
    public Boolean getGranted() { return granted; }

    public void setGranted(Boolean granted) { this.granted = granted; }
}
