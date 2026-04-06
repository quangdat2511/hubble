package com.example.hubble.data.model.server;

import com.example.hubble.data.model.dm.ChannelDto;

public class ChannelEvent {
    private String type;
    private ChannelDto channel;

    public String getType() { return type; }
    public ChannelDto getChannel() { return channel; }
}
