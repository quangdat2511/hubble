package com.hubble.mapper;

import com.hubble.dto.response.NotificationResponse;
import com.hubble.entity.Notification;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface NotificationMapper {
    NotificationResponse toResponse(Notification notification);
}