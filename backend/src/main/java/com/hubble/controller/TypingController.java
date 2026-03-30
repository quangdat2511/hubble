package com.hubble.controller;

import com.hubble.dto.request.TypingEvent;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TypingController {

    SimpMessagingTemplate messagingTemplate;

    /**
     * Receives a typing event from a client via STOMP:
     *   destination: /app/channels/{channelId}/typing
     *   payload:     { "userId": "<sender-id>" }
     *
     * Broadcasts to all subscribers of the channel typing topic:
     *   /topic/channels/{channelId}/typing
     *
     * The Android client filters out its own userId so only the peer sees
     * the "... is typing" indicator.
     */
    @MessageMapping("/channels/{channelId}/typing")
    public void handleTyping(
            @DestinationVariable String channelId,
            @Payload TypingEvent event
    ) {
        messagingTemplate.convertAndSend(
                "/topic/channels/" + channelId + "/typing",
                event
        );
    }
}
