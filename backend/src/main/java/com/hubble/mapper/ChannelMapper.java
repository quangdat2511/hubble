package com.hubble.mapper;

import com.hubble.dto.response.ChannelResponse;
import com.hubble.entity.Channel;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ChannelMapper {
    ChannelResponse toChannelResponse(Channel channel);
}
