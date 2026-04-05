package com.hubble.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChannelRoleResponse {
    private String roleId;
    private String name;
    private Integer color;
    private Integer memberCount;
}
