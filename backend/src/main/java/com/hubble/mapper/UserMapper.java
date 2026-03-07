package com.hubble.mapper;

import com.hubble.dto.response.UserResponse;
import com.hubble.entity.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserResponse toUserResponse(User user);
}