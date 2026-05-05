package com.hubble.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hubble.dto.request.GroqChatRequest;
import com.hubble.dto.response.GroqChatResponse;
import com.hubble.dto.response.SmartReplyResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class SmartReplyService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${groq.endpoint}")
    private String groqApiUrl;

    @Value("${groq.api-key}")
    private String apiKey;

    public SmartReplyResponse generateSuggestions(String messageContent) {
        if (messageContent == null || messageContent.trim().length() < 3) {
            return null;
        }

        String systemPrompt = "You are a smart chat assistant. Based on the user's message, perform 2 tasks:\n" +
                "1. Analyze the context/emotion of the message.\n" +
                "2. Suggest 3 short reply options (under 6 words each) IN THE EXACT SAME LANGUAGE as the user's message.\n" +
                "CRITICAL: You MUST return data in JSON object format containing exactly 2 keys: 'contextTag' (string) and 'suggestions' (array of strings).\n" +
                "Example output: {\"contextTag\": \"Greeting\", \"suggestions\": [\"Hello\", \"Hi there!\", \"How are you?\"]}";
        try {
            GroqChatRequest requestBody = GroqChatRequest.builder()
                    .model("llama-3.1-8b-instant")
                    .temperature(0.7)
                    .response_format(Map.of("type", "json_object"))
                    .messages(List.of(
                            GroqChatRequest.Message.builder().role("system").content(systemPrompt).build(),
                            GroqChatRequest.Message.builder().role("user").content(messageContent).build()
                    ))
                    .build();

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + apiKey);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<GroqChatRequest> entity = new HttpEntity<>(requestBody, headers);

            GroqChatResponse response = restTemplate.postForObject(groqApiUrl, entity, GroqChatResponse.class);

            return parseStructuredResponse(response);

        } catch (Exception e) {
            log.error("Lỗi khi gọi Groq API: {}", e.getMessage());
            return null;
        }
    }

    private SmartReplyResponse parseStructuredResponse(GroqChatResponse response) {
        try {
            if (response != null && response.getChoices() != null && !response.getChoices().isEmpty()) {
                String aiContent = response.getChoices().get(0).getMessage().getContent();
                if (aiContent == null || aiContent.isBlank()) return null;

                return objectMapper.readValue(aiContent, SmartReplyResponse.class);
            }
        } catch (Exception e) {
            log.error("Lỗi khi parse Structured JSON: ", e);
        }
        return null;
    }
}