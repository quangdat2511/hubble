package com.hubble.dto.request;

import com.hubble.enums.ChannelType;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreateChannelRequest {
    String name;
    ChannelType type;
    UUID parentId;
    Boolean isPrivate;
    List<UUID> memberIds;
    List<UUID> roleIds;
}
