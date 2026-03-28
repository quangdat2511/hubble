package com.hubble.mapper;

import com.hubble.dto.response.RoleResponse;
import com.hubble.entity.Role;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface RoleMapper {

    @Mapping(target = "memberCount", ignore = true)
    RoleResponse toRoleResponse(Role role);
}
