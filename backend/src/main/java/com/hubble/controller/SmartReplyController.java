package com.hubble.controller;

import com.hubble.dto.response.SmartReplyResponse;
import com.hubble.service.SmartReplyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class SmartReplyController {

    private final SmartReplyService smartReplyService;

    @PostMapping("/smart-reply")
    public ResponseEntity<SmartReplyResponse> getSmartReply(@RequestBody Map<String, String> request) {
        String content = request.get("content");

        SmartReplyResponse response = smartReplyService.generateSuggestions(content);

        if (response == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(response);
    }
}