package com.hubble.mapper;

import com.hubble.dto.response.MessageResponse;
import com.hubble.entity.Message;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface MessageMapper {

    @Mapping(target = "id", expression = "java(message.getId().toString())")
    @Mapping(target = "channelId", expression = "java(message.getChannelId().toString())")
    @Mapping(target = "authorId", expression = "java(message.getAuthorId().toString())")
    @Mapping(target = "replyToId", expression = "java(message.getReplyToId() != null ? message.getReplyToId().toString() : null)")
    @Mapping(target = "type", expression = "java(message.getType().name())")
    @Mapping(target = "createdAt", expression = "java(message.getCreatedAt() != null ? message.getCreatedAt().toString() : null)")
    @Mapping(target = "editedAt", expression = "java(message.getEditedAt() != null ? message.getEditedAt().toString() : null)")
    MessageResponse toMessageResponse(Message message);
}
