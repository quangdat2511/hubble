package com.hubble.controller;

import com.hubble.dto.request.DeliveryEvent;
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
public class DeliveryController {

    SimpMessagingTemplate messagingTemplate;

    /**
     * Receives a delivery ack from a client via STOMP:
     *   destination: /app/channels/{channelId}/delivered
     *   payload:     { "userId": "<sender-id>" }
     *
     * Broadcasts to all subscribers so the message sender knows their messages
     * have been delivered (the recipient has opened the conversation).
     *   /topic/channels/{channelId}/delivery
     */
    @MessageMapping("/channels/{channelId}/delivered")
    public void handleDelivery(
            @DestinationVariable String channelId,
            @Payload DeliveryEvent event
    ) {
        messagingTemplate.convertAndSend(
                "/topic/channels/" + channelId + "/delivery",
                event
        );
    }
}
