package com.hubble.dto.request;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class GroqChatRequest {
    private String model;
    private double temperature;
    private Map<String, String> response_format; // Dành cho JSON Mode
    private List<Message> messages;

    @Data
    @Builder
    public static class Message {
        private String role;
        private String content;
    }
}