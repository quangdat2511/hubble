package com.hubble.controller;


import org.springframework.data.domain.Page;
import com.hubble.dto.request.MessageRequest;
import com.hubble.entity.Message;
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
    public ResponseEntity<Message> sendMessage(@RequestBody MessageRequest request) {
        Message savedMessage = messageService.sendMessage(request);
        return ResponseEntity.ok(savedMessage);
    }

    @GetMapping("/{channelId}")
    public ResponseEntity<List<Message>> getMessages(
            @PathVariable UUID channelId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size
    ) {
        List<Message> messages = messageService.getMessagesByChannel(channelId, page, size);
        return ResponseEntity.ok(messages);
    }
}
