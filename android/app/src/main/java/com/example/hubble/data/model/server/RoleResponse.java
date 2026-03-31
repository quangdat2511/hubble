package com.example.hubble.data.model.server;

import com.google.gson.annotations.SerializedName;

/**
 * Maps to backend RoleResponse DTO.
 */
public class RoleResponse {
    private String id;
    private String serverId;
    private String name;
    private Integer color;
    private Long permissions;
    private Integer position;
    @SerializedName("isDefault")
    private Boolean isDefault;
    private Boolean displaySeparately;
    private Boolean mentionable;
    private Integer memberCount;
    private String createdAt;

    public String getId() { return id; }
    public String getServerId() { return serverId; }
    public String getName() { return name; }
    public Integer getColor() { return color; }
    public Long getPermissions() { return permissions; }
    public Integer getPosition() { return position; }
    public Boolean getIsDefault() { return isDefault; }
    public Boolean getDisplaySeparately() { return displaySeparately; }
    public Boolean getMentionable() { return mentionable; }
    public Integer getMemberCount() { return memberCount; }
    public String getCreatedAt() { return createdAt; }
}
