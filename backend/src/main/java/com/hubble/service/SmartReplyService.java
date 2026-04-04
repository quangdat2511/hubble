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

    public List<String> generateSuggestions(String messageContent) {
        if (messageContent == null || messageContent.trim().length() < 3) {
            return List.of();
        }

        String systemPrompt = "Bạn là trợ lý chat. Dựa vào tin nhắn sau, gợi ý 3 câu trả lời ngắn gọn (dưới 6 từ) bằng tiếng Việt. " +
                "BẮT BUỘC trả về dữ liệu dưới định dạng JSON object có chứa key 'suggestions' là một mảng chuỗi. " +
                "Ví dụ: {\"suggestions\": [\"Ok bạn\", \"Để mình xem\", \"Tuyệt\"]}";

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
            return List.of();
        }
    }

    private List<String> parseStructuredResponse(GroqChatResponse response) {
        try {
            if (response != null && response.getChoices() != null && !response.getChoices().isEmpty()) {
                String aiContent = response.getChoices().get(0).getMessage().getContent();
                if (aiContent == null || aiContent.isBlank()) return List.of();

                SmartReplyResponse replyDto = objectMapper.readValue(aiContent, SmartReplyResponse.class);
                return replyDto.getSuggestions() != null ? replyDto.getSuggestions() : List.of();
            }
        } catch (Exception e) {
            log.error("Lỗi khi parse Structured JSON: {}", e.getMessage());
        }
        return List.of();
    }
}