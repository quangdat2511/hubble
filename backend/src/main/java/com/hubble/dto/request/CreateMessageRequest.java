package com.hubble.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateMessageRequest {
    private String channelId;
    private String replyToId;
    private String content;
}
