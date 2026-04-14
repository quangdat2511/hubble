package com.hubble.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TypingEvent {
    private String userId;
    /** Optional: sent by client so receivers can show "Alice is typing…" without a user lookup. */
    private String username;
    private String displayName;
}
