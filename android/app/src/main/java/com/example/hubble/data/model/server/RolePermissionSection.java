package com.example.hubble.data.model.server;

import java.util.List;

/**
 * Represents a section header + list of permissions in the role permission screen.
 */
public class RolePermissionSection {
    private final String title;
    private final List<RolePermissionItem> permissions;

    public RolePermissionSection(String title, List<RolePermissionItem> permissions) {
        this.title = title;
        this.permissions = permissions;
    }

    public String getTitle() { return title; }
    public List<RolePermissionItem> getPermissions() { return permissions; }
}
