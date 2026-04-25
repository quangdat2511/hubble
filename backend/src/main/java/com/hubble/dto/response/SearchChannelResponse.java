package com.hubble.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

/** Search result for a server channel. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SearchChannelResponse {
    String id;
    String name;
    String type;
    String topic;
}
