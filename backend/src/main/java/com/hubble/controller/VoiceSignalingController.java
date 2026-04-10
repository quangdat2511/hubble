package com.hubble.controller;

import com.hubble.dto.common.ApiResponse;
import com.hubble.dto.request.VoiceSignalMessage;
import com.hubble.dto.response.VoiceParticipant;
import com.hubble.service.VoiceStateManager;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Slf4j
@Controller
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class VoiceSignalingController {

    SimpMessagingTemplate messagingTemplate;
    VoiceStateManager voiceStateManager;

    /**
     * Client sends: /app/voice/{channelId}/join
     * Broadcasts to: /topic/voice/{channelId}
     */
    @MessageMapping("/voice/{channelId}/join")
    public void handleJoin(
            @DestinationVariable String channelId,
            @Payload VoiceSignalMessage message
    ) {
        log.info("[VOICE] JOIN: user={} channel={}", message.getUserId(), channelId);
        message.setType("join");
        message.setChannelId(channelId);

        voiceStateManager.addParticipant(channelId, VoiceParticipant.builder()
                .userId(message.getUserId())
                .displayName(message.getDisplayName())
                .avatarUrl(message.getAvatarUrl())
                .build());

        messagingTemplate.convertAndSend("/topic/voice/" + channelId, message);
    }

    /**
     * Client sends: /app/voice/{channelId}/leave
     * Broadcasts to: /topic/voice/{channelId}
     */
    @MessageMapping("/voice/{channelId}/leave")
    public void handleLeave(
            @DestinationVariable String channelId,
            @Payload VoiceSignalMessage message
    ) {
        log.info("[VOICE] LEAVE: user={} channel={}", message.getUserId(), channelId);
        message.setType("leave");
        message.setChannelId(channelId);

        voiceStateManager.removeParticipant(channelId, message.getUserId());

        messagingTemplate.convertAndSend("/topic/voice/" + channelId, message);
    }

    /**
     * Client sends: /app/voice/{channelId}/offer
     * Routed to specific peer: /topic/voice/{channelId}/user/{targetUserId}
     */
    @MessageMapping("/voice/{channelId}/offer")
    public void handleOffer(
            @DestinationVariable String channelId,
            @Payload VoiceSignalMessage message
    ) {
        log.info("[VOICE] OFFER: from={} to={} channel={}", message.getUserId(), message.getTargetUserId(), channelId);
        message.setType("offer");
        message.setChannelId(channelId);
        messagingTemplate.convertAndSend(
                "/topic/voice/" + channelId + "/user/" + message.getTargetUserId(),
                message);
    }

    /**
     * Client sends: /app/voice/{channelId}/answer
     * Routed to specific peer: /topic/voice/{channelId}/user/{targetUserId}
     */
    @MessageMapping("/voice/{channelId}/answer")
    public void handleAnswer(
            @DestinationVariable String channelId,
            @Payload VoiceSignalMessage message
    ) {
        log.info("[VOICE] ANSWER: from={} to={} channel={}", message.getUserId(), message.getTargetUserId(), channelId);
        message.setType("answer");
        message.setChannelId(channelId);
        messagingTemplate.convertAndSend(
                "/topic/voice/" + channelId + "/user/" + message.getTargetUserId(),
                message);
    }

    /**
     * Client sends: /app/voice/{channelId}/ice-candidate
     * Routed to specific peer: /topic/voice/{channelId}/user/{targetUserId}
     */
    @MessageMapping("/voice/{channelId}/ice-candidate")
    public void handleIceCandidate(
            @DestinationVariable String channelId,
            @Payload VoiceSignalMessage message
    ) {
        log.info("[VOICE] ICE-CANDIDATE: from={} to={} channel={}", message.getUserId(), message.getTargetUserId(), channelId);
        message.setType("ice-candidate");
        message.setChannelId(channelId);
        messagingTemplate.convertAndSend(
                "/topic/voice/" + channelId + "/user/" + message.getTargetUserId(),
                message);
    }

    /**
     * REST endpoint to get current voice channel participants.
     */
    @GetMapping("/api/voice/{channelId}/participants")
    @ResponseBody
    public ApiResponse<List<VoiceParticipant>> getParticipants(@PathVariable String channelId) {
        List<VoiceParticipant> participants = voiceStateManager.getParticipants(channelId);
        log.info("[VOICE] GET_PARTICIPANTS: channel={} count={}", channelId, participants.size());
        return ApiResponse.<List<VoiceParticipant>>builder()
                .result(participants)
                .build();
    }
}
