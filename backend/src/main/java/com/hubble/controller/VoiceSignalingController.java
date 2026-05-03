package com.hubble.controller;

import com.hubble.dto.common.ApiResponse;
import com.hubble.dto.response.VoiceParticipant;
import com.hubble.security.UserPrincipal;
import com.hubble.service.VoiceStateManager;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.stereotype.Controller;

import java.util.List;

@Slf4j
@Controller
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class VoiceSignalingController {

    VoiceStateManager voiceStateManager;

    /**
     * REST endpoint to get current voice channel participants.
     * Participants are now managed by Agora callbacks (join/leave events
     * are handled client-side; server state is updated via AgoraTokenController
     * or future webhook integration).
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

    @DeleteMapping("/api/voice/{channelId}/participants")
    @ResponseBody
    public ApiResponse<Void> leaveChannel(@PathVariable String channelId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        String userId = principal.getId().toString();
        voiceStateManager.removeParticipant(channelId, userId);
        log.info("[VOICE] LEAVE: channel={} userId={}", channelId, userId);
        return ApiResponse.<Void>builder().build();
    }

    /**
     * Heartbeat: client calls this every ~15 s while in a voice call.
     * The backend uses the timestamp to detect crashed/killed clients and
     * remove them from the participant list after 30 s of silence.
     */
    @PostMapping("/api/voice/{channelId}/heartbeat")
    @ResponseBody
    public ApiResponse<Void> heartbeat(@PathVariable String channelId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        String userId = principal.getId().toString();
        boolean found = voiceStateManager.heartbeat(channelId, userId);
        if (!found) {
            log.warn("[VOICE] HEARTBEAT missed (not in channel): channel={} userId={}", channelId, userId);
        }
        return ApiResponse.<Void>builder().build();
    }
}
