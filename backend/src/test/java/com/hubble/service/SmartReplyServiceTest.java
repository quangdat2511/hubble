package com.hubble.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hubble.dto.response.GroqChatResponse;
import com.hubble.dto.response.SmartReplyResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SmartReplyServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private ObjectMapper objectMapper;

    // Dùng deep stubs để mock dễ dàng các chuỗi get lồng nhau (response.getChoices().get(0)...)
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private GroqChatResponse mockGroqResponse;

    @InjectMocks
    private SmartReplyService smartReplyService;

    @BeforeEach
    void setUp() {
        // "Tiêm" mock object và giá trị @Value vào class bằng Reflection
        ReflectionTestUtils.setField(smartReplyService, "restTemplate", restTemplate);
        ReflectionTestUtils.setField(smartReplyService, "objectMapper", objectMapper);
        ReflectionTestUtils.setField(smartReplyService, "groqApiUrl", "https://api.groq.com");
        ReflectionTestUtils.setField(smartReplyService, "apiKey", "test-api-key");
    }

    @Test
    void generateSuggestions_ContentTooShort_ReturnsNull() {
        // Act & Assert
        assertNull(smartReplyService.generateSuggestions(null));
        assertNull(smartReplyService.generateSuggestions(""));
        assertNull(smartReplyService.generateSuggestions("Hi")); // < 3 chars
    }

    @Test
    void generateSuggestions_ApiCallSuccess_ReturnsParsedResponse() throws Exception {
        // Arrange
        String validJson = "{\"contextTag\":\"Greeting\",\"suggestions\":[\"Hi\",\"Hello\"]}";

        when(mockGroqResponse.getChoices().get(0).getMessage().getContent()).thenReturn(validJson);
        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(GroqChatResponse.class)))
                .thenReturn(mockGroqResponse);

        SmartReplyResponse expectedResponse = new SmartReplyResponse();
        // Giả lập ObjectMapper parse thành công
        when(objectMapper.readValue(validJson, SmartReplyResponse.class)).thenReturn(expectedResponse);

        // Act
        SmartReplyResponse actualResponse = smartReplyService.generateSuggestions("Xin chào bạn");

        // Assert
        assertNotNull(actualResponse);
        assertEquals(expectedResponse, actualResponse);
    }

    @Test
    void generateSuggestions_ApiThrowsException_ReturnsNull() {
        // Arrange
        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(GroqChatResponse.class)))
                .thenThrow(new RestClientException("Connection Timeout"));

        // Act
        SmartReplyResponse response = smartReplyService.generateSuggestions("Xin chào bạn");

        // Assert
        assertNull(response);
    }

    @Test
    void generateSuggestions_InvalidJson_ReturnsNull() throws Exception {
        // Arrange
        String invalidJson = "Invalid JSON string";

        when(mockGroqResponse.getChoices().get(0).getMessage().getContent()).thenReturn(invalidJson);
        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(GroqChatResponse.class)))
                .thenReturn(mockGroqResponse);

        when(objectMapper.readValue(invalidJson, SmartReplyResponse.class))
                .thenThrow(new RuntimeException("Parse error"));

        // Act
        SmartReplyResponse response = smartReplyService.generateSuggestions("Xin chào bạn");

        // Assert
        assertNull(response);
    }
}