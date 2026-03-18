package com.hubble.dto.event;

import com.hubble.dto.response.MessageResponse;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MessageEvent {
    String action;          // "SEND" | "EDIT" | "DELETE"
    MessageResponse message;
}
