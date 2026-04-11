package com.hubble.controller;

import com.hubble.dto.request.TypingEvent;
import com.hubble.testsupport.MessagingTestSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TypingControllerTest {

    @Spy
    SimpMessagingTemplate messagingTemplate = MessagingTestSupport.createTemplate();

    @InjectMocks TypingController typingController;

    @Test
    void handleTyping_relaysPayloadToChannelTypingTopic() {
        String channelId = "channel-uuid";
        TypingEvent event = new TypingEvent();
        event.setUserId("user-1");

        typingController.handleTyping(channelId, event);

        verify(messagingTemplate).convertAndSend("/topic/channels/" + channelId + "/typing", event);
    }
}
