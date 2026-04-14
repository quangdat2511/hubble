package com.hubble.testsupport;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.support.ExecutorSubscribableChannel;

/**
 * Real {@link SimpMessagingTemplate} for unit tests — avoids Mockito mocking JDK 25–incompatible types.
 */
public final class MessagingTestSupport {

    private MessagingTestSupport() {}

    public static SimpMessagingTemplate createTemplate() {
        return new SimpMessagingTemplate(new ExecutorSubscribableChannel());
    }
}
