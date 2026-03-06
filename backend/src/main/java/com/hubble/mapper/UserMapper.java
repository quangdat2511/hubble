package com.hubble.mapper;

import com.hubble.dto.request.CreateUserRequest;
import com.hubble.dto.response.UserResponse;
import com.hubble.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "avatarUrl", ignore = true)
    @Mapping(target = "bio", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "customStatus", ignore = true)
    @Mapping(target = "lastSeenAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    User toUser(CreateUserRequest request);

    UserResponse toUserResponse(User user);
}