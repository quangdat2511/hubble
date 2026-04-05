package com.hubble.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateChannelRequest {
    private String name;
    private String topic;
    private UUID parentId;
    private Boolean isPrivate;
}
