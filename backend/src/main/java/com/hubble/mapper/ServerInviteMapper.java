package com.hubble.mapper;

import com.hubble.dto.response.ServerInviteResponse;
import com.hubble.entity.Server;
import com.hubble.entity.ServerInvite;
import com.hubble.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ServerInviteMapper {

    @Mapping(target = "id",              expression = "java(invite.getId().toString())")
    @Mapping(target = "serverId",        expression = "java(invite.getServerId().toString())")
    @Mapping(target = "serverName",      source = "server.name")
    @Mapping(target = "serverIconUrl",   source = "server.iconUrl")
    @Mapping(target = "inviterId",       expression = "java(invite.getInviterId().toString())")
    @Mapping(target = "inviterUsername", source = "inviter.username")
    @Mapping(target = "inviterDisplayName", source = "inviter.displayName")
    @Mapping(target = "inviteeId",       expression = "java(invite.getInviteeId().toString())")
    @Mapping(target = "inviteeUsername", source = "invitee.username")
    @Mapping(target = "inviteeDisplayName", source = "invitee.displayName")
    @Mapping(target = "status",          expression = "java(invite.getStatus().name())")
    @Mapping(target = "createdAt",       source = "invite.createdAt")
    @Mapping(target = "expiresAt",       source = "invite.expiresAt")
    @Mapping(target = "respondedAt",     source = "invite.respondedAt")
    ServerInviteResponse toResponse(ServerInvite invite, Server server, User inviter, User invitee);
}

