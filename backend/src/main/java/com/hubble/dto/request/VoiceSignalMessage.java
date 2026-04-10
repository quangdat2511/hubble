package com.hubble.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VoiceSignalMessage {
    private String type;       // "join", "leave", "offer", "answer", "ice-candidate"
    private String channelId;
    private String userId;
    private String displayName;
    private String avatarUrl;
    private String targetUserId; // for offer/answer/ice — the peer
    private String sdp;          // for offer/answer
    private String candidate;    // for ice-candidate
    private String sdpMid;
    private Integer sdpMLineIndex;
}
