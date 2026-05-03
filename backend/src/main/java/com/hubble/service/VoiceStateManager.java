package com.hubble.service;

import com.hubble.dto.response.VoiceParticipant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Service
public class VoiceStateManager {

    /** Participants with no heartbeat for more than this many ms are considered crashed/gone. */
    private static final long STALE_THRESHOLD_MS = 30_000;

    // channelId → list of participants
    private final Map<String, CopyOnWriteArrayList<VoiceParticipant>> channelParticipants =
            new ConcurrentHashMap<>();

    public void addParticipant(String channelId, VoiceParticipant participant) {
        channelParticipants.computeIfAbsent(channelId, k -> new CopyOnWriteArrayList<>());
        CopyOnWriteArrayList<VoiceParticipant> participants = channelParticipants.get(channelId);
        // Stamp the current time so the stale-cleanup scheduler knows when they joined
        participant.setLastHeartbeatAt(System.currentTimeMillis());
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

    /**
     * Refresh the heartbeat timestamp for a participant.
     * @return false if the participant was not found (e.g., already cleaned up)
     */
    public boolean heartbeat(String channelId, String userId) {
        CopyOnWriteArrayList<VoiceParticipant> participants = channelParticipants.get(channelId);
        if (participants == null) return false;
        for (VoiceParticipant p : participants) {
            if (p.getUserId().equals(userId)) {
                p.setLastHeartbeatAt(System.currentTimeMillis());
                return true;
            }
        }
        return false;
    }

    public List<VoiceParticipant> getParticipants(String channelId) {
        CopyOnWriteArrayList<VoiceParticipant> participants = channelParticipants.get(channelId);
        return participants != null ? List.copyOf(participants) : List.of();
    }

    /**
     * Runs every 15 s. Removes any participant whose last heartbeat is older than
     * STALE_THRESHOLD_MS — this handles app crash / process kill where the leave
     * API is never called.
     */
    @Scheduled(fixedDelay = 15_000)
    public void removeStaleParticipants() {
        long cutoff = System.currentTimeMillis() - STALE_THRESHOLD_MS;
        channelParticipants.forEach((channelId, participants) -> {
            int before = participants.size();
            participants.removeIf(p -> p.getLastHeartbeatAt() > 0 && p.getLastHeartbeatAt() < cutoff);
            int removed = before - participants.size();
            if (removed > 0) {
                log.info("[VOICE] Stale cleanup: channel={} removed={} remaining={}",
                        channelId, removed, participants.size());
            }
            if (participants.isEmpty()) {
                channelParticipants.remove(channelId);
            }
        });
    }
}
