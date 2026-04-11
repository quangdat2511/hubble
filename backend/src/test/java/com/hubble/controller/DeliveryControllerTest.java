package com.hubble.controller;

import com.hubble.dto.request.DeliveryEvent;
import com.hubble.testsupport.MessagingTestSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DeliveryControllerTest {

    @Spy
    SimpMessagingTemplate messagingTemplate = MessagingTestSupport.createTemplate();

    @InjectMocks DeliveryController deliveryController;

    @Test
    void handleDelivery_relaysAckToChannelDeliveryTopic() {
        String channelId = "channel-uuid";
        DeliveryEvent event = new DeliveryEvent();
        event.setUserId("sender-for-ack");

        deliveryController.handleDelivery(channelId, event);

        verify(messagingTemplate).convertAndSend("/topic/channels/" + channelId + "/delivery", event);
    }
}
