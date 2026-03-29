package com.hubble.mapper;

import com.hubble.dto.response.AttachmentResponse;
import com.hubble.entity.Attachment;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AttachmentMapper {
    AttachmentResponse toAttachmentResponse(Attachment attachment);
}
