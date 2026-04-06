package com.hubble.mapper;

import com.hubble.dto.request.CreateMessageRequest;
import com.hubble.dto.response.AttachmentResponse;
import com.hubble.dto.response.MessageResponse;
import com.hubble.entity.Message;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;
import java.util.UUID;

@Mapper(componentModel = "spring")
public interface MessageMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "type", ignore = true)
    @Mapping(target = "isPinned", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    @Mapping(target = "editedAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "authorId", source = "authorId")
    Message toMessage(CreateMessageRequest request, UUID authorId);

    @Mapping(target = "attachments", ignore = true)
    @Mapping(target = "reactions", ignore = true)
    MessageResponse toMessageResponse(Message message);

    default MessageResponse toMessageResponse(Message message, List<AttachmentResponse> attachments) {
        MessageResponse response = toMessageResponse(message);
        response.setAttachments(attachments);
        return response;
    }
}