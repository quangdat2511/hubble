package com.example.hubble.data.model.server;

public class CreateInviteRequest {
    private String inviteeUsername;

    public CreateInviteRequest(String inviteeUsername) {
        this.inviteeUsername = inviteeUsername;
    }

    public String getInviteeUsername() { return inviteeUsername; }
    public void setInviteeUsername(String inviteeUsername) { this.inviteeUsername = inviteeUsername; }
}

