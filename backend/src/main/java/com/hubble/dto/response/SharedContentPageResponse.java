package com.hubble.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SharedContentPageResponse {
    String type;
    int page;
    int size;
    boolean hasMore;
    List<SharedContentItemResponse> items;
}
