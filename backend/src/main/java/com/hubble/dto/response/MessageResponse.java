package com.hubble.dto.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MessageResponse {
    private String id;
    private String channelId;
    private String authorId;
    private String replyToId;
    private String content;
    private String type;
    private Boolean isPinned;
    private Boolean isDeleted;
    private String editedAt;
    private String createdAt;
}
