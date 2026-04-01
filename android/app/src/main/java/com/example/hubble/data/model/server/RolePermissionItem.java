package com.example.hubble.data.model.server;

/**
 * Represents a single permission toggle item used in role permission screens.
 */
public class RolePermissionItem {
    private final String key;
    private final String name;
    private final String description;
    private boolean enabled;

    public RolePermissionItem(String key, String name, String description, boolean enabled) {
        this.key = key;
        this.name = name;
        this.description = description;
        this.enabled = enabled;
    }

    public String getKey() { return key; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}
