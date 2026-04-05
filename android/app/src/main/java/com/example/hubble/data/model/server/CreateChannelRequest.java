package com.example.hubble.data.model.server;

import java.util.List;

public class CreateChannelRequest {
    private String name;
    private String type;
    private String parentId;
    private Boolean isPrivate;
    private List<String> memberIds;
    private List<String> roleIds;

    public CreateChannelRequest() {}

    public CreateChannelRequest(String name, String type, String parentId, Boolean isPrivate,
                                List<String> memberIds, List<String> roleIds) {
        this.name = name;
        this.type = type;
        this.parentId = parentId;
        this.isPrivate = isPrivate;
        this.memberIds = memberIds;
        this.roleIds = roleIds;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getParentId() { return parentId; }
    public void setParentId(String parentId) { this.parentId = parentId; }

    public Boolean getIsPrivate() { return isPrivate; }
    public void setIsPrivate(Boolean isPrivate) { this.isPrivate = isPrivate; }

    public List<String> getMemberIds() { return memberIds; }
    public void setMemberIds(List<String> memberIds) { this.memberIds = memberIds; }

    public List<String> getRoleIds() { return roleIds; }
    public void setRoleIds(List<String> roleIds) { this.roleIds = roleIds; }
}
