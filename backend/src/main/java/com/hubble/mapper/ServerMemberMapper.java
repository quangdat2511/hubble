package com.hubble.mapper;

import com.hubble.dto.response.ServerMemberResponse;
import com.hubble.entity.Server;
import com.hubble.entity.ServerMember;
import com.hubble.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ServerMemberMapper {

    @Mapping(target = "userId",      expression = "java(member.getUserId().toString())")
    @Mapping(target = "username",    expression = "java(user != null ? user.getUsername() : \"Unknown\")")
    @Mapping(target = "displayName", expression = "java(user != null ? user.getDisplayName() : null)")
    @Mapping(target = "avatarUrl",   expression = "java(user != null ? user.getAvatarUrl() : null)")
    @Mapping(target = "status",      expression = "java(user != null && user.getStatus() != null ? user.getStatus().name() : \"OFFLINE\")")
    @Mapping(target = "isOwner",     expression = "java(server.getOwnerId().equals(member.getUserId()))")
    @Mapping(target = "roles",       expression = "java(new java.util.ArrayList<>())")
    ServerMemberResponse toResponse(ServerMember member, User user, Server server);
}


