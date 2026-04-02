package com.hubble.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReorderRolesRequest {
    List<RolePositionEntry> positions;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RolePositionEntry {
        @NotNull UUID roleId;
        @NotNull Integer position;
    }
}
