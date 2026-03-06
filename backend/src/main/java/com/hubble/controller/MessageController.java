package com.hubble.controller;

import com.hubble.dto.request.CreateMessageRequest;
import com.hubble.dto.response.MessageResponse;
import com.hubble.dto.common.ApiResponse;
import com.hubble.service.MessageService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;


@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class MessageController {

    MessageService messageService;

    @PostMapping
    public ResponseEntity<ApiResponse<MessageResponse>> sendMessage(@RequestBody CreateMessageRequest request) {
        MessageResponse messageResponse = messageService.sendMessage(request);
        return ResponseEntity.ok(ApiResponse.<MessageResponse>builder()
                .result(messageResponse)
                .build());
    }

    @GetMapping("/{channelId}")
    public ResponseEntity<ApiResponse<List<MessageResponse>>> getMessages(
            @PathVariable UUID channelId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size
    ) {
        List<MessageResponse> messages = messageService.getMessagesByChannel(channelId, page, size);
        return ResponseEntity.ok(ApiResponse.<List<MessageResponse>>builder()
                .result(messages)
                .build());
    }
}
