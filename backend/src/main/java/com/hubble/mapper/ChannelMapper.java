package com.hubble.mapper;

import com.hubble.dto.response.ChannelResponse;
import com.hubble.entity.Channel;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ChannelMapper {

    @Mapping(target = "peerUserId", ignore = true)
    @Mapping(target = "peerUsername", ignore = true)
    @Mapping(target = "peerDisplayName", ignore = true)
    @Mapping(target = "peerAvatarUrl", ignore = true)
    @Mapping(target = "peerStatus", ignore = true)
    @Mapping(target = "unreadCount", ignore = true)
    @Mapping(target = "id", expression = "java(channel.getId() != null ? channel.getId().toString() : null)")
    @Mapping(target = "serverId", expression = "java(channel.getServerId() != null ? channel.getServerId().toString() : null)")
    @Mapping(target = "parentId", expression = "java(channel.getParentId() != null ? channel.getParentId().toString() : null)")
    @Mapping(target = "type", expression = "java(channel.getType() != null ? channel.getType().name() : null)")
    @Mapping(target = "createdAt", expression = "java(channel.getCreatedAt() != null ? channel.getCreatedAt().toString() : null)")
    ChannelResponse toChannelResponse(Channel channel);
}
