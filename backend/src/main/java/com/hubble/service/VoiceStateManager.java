package com.hubble.service;

import com.hubble.dto.response.VoiceParticipant;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class VoiceStateManager {

    // channelId → list of participants
    private final Map<String, CopyOnWriteArrayList<VoiceParticipant>> channelParticipants =
            new ConcurrentHashMap<>();

    public void addParticipant(String channelId, VoiceParticipant participant) {
        channelParticipants.computeIfAbsent(channelId, k -> new CopyOnWriteArrayList<>());
        CopyOnWriteArrayList<VoiceParticipant> participants = channelParticipants.get(channelId);
        // Remove old entry for same user (reconnect case)
        participants.removeIf(p -> p.getUserId().equals(participant.getUserId()));
        participants.add(participant);
    }

    public void removeParticipant(String channelId, String userId) {
        CopyOnWriteArrayList<VoiceParticipant> participants = channelParticipants.get(channelId);
        if (participants != null) {
            participants.removeIf(p -> p.getUserId().equals(userId));
            if (participants.isEmpty()) {
                channelParticipants.remove(channelId);
            }
        }
    }

    public List<VoiceParticipant> getParticipants(String channelId) {
        CopyOnWriteArrayList<VoiceParticipant> participants = channelParticipants.get(channelId);
        return participants != null ? List.copyOf(participants) : List.of();
    }
}
