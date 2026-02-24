package com.hubble.mapper;

import com.hubble.dto.request.UserCreationRequest;
import com.hubble.dto.request.UserUpdateRequest;
import com.hubble.dto.response.UserResponse;
import com.hubble.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface UserMapper {

    User toUser(UserCreationRequest request);

    UserResponse toUserResponse(User user);

    void updateUser(@MappingTarget User user, UserUpdateRequest request);
}
