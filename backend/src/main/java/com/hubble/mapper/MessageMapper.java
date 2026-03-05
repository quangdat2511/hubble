package com.hubble.mapper;

import com.hubble.dto.request.MessageRequest;
import com.hubble.entity.Message;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface MessageMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "type", ignore = true)
    @Mapping(target = "isPinned", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    @Mapping(target = "editedAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    Message toMessage(MessageRequest request);

}