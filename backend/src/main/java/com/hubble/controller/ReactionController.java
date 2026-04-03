package com.hubble.controller;

import com.hubble.dto.common.ApiResponse;
import com.hubble.dto.request.ToggleReactionRequest;
import com.hubble.dto.response.ReactionResponse;
import com.hubble.entity.Message;
import com.hubble.exception.AppException;
import com.hubble.exception.ErrorCode;
import com.hubble.repository.MessageRepository;
import com.hubble.security.UserPrincipal;
import com.hubble.service.ReactionService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ReactionController {

    ReactionService reactionService;
    MessageRepository messageRepository;
    SimpMessagingTemplate messagingTemplate;

    @PutMapping("/{messageId}/reactions")
    public ApiResponse<List<ReactionResponse>> toggleReaction(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String messageId,
            @RequestBody ToggleReactionRequest request
    ) {
        UUID msgId = UUID.fromString(messageId);
        Message message = messageRepository.findById(msgId)
                .orElseThrow(() -> new AppException(ErrorCode.MESSAGE_NOT_FOUND));

        List<ReactionResponse> reactions =
                reactionService.toggleReaction(msgId, principal.getId(), request.getEmoji());

        messagingTemplate.convertAndSend(
                "/topic/channels/" + message.getChannelId() + "/reactions",
                Map.of("messageId", messageId, "reactions", reactions)
        );

        return ApiResponse.<List<ReactionResponse>>builder()
                .result(reactions)
                .build();
    }
}
