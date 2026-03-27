package com.hubble.mapper;

import com.hubble.dto.response.ServerResponse;
import com.hubble.entity.Server;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ServerMapper {
    ServerResponse toServerResponse(Server server);
}

