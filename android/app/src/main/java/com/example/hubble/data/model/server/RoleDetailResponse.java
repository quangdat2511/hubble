package com.example.hubble.data.model.server;

import java.util.List;

/**
 * Maps to backend RoleDetailResponse DTO.
 */
public class RoleDetailResponse {
    private String id;
    private String name;
    private Integer color;
    private Long permissions;
    private Boolean displaySeparately;
    private Boolean mentionable;
    private Integer memberCount;
    private List<PermissionResponse> permissionDetails;
    private List<MemberBriefResponse> members;

    public String getId() { return id; }
    public String getName() { return name; }
    public Integer getColor() { return color; }
    public Long getPermissions() { return permissions; }
    public Boolean getDisplaySeparately() { return displaySeparately; }
    public Boolean getMentionable() { return mentionable; }
    public Integer getMemberCount() { return memberCount; }
    public List<PermissionResponse> getPermissionDetails() { return permissionDetails; }
    public List<MemberBriefResponse> getMembers() { return members; }
}
