package com.hubble.service;

import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Maps UUID user IDs to Agora uint32 UIDs deterministically.
 *
 * Strategy: XOR the two 64-bit halves of the UUID, then mask to 31 bits
 * to stay within Agora's safe range. UID 0 is reserved by Agora and is
 * remapped to 1.
 *
 * The mapping is purely in-memory; no DB migration is needed.
 */
@Component
public class AgoraUidMapper {

    /**
     * Converts a UUID to a stable, non-zero Agora UID.
     */
    public int toAgoraUid(UUID userId) {
        long hash = userId.getMostSignificantBits() ^ userId.getLeastSignificantBits();
        int uid = (int) (hash & 0x7FFFFFFFL);
        return uid == 0 ? 1 : uid;
    }

    /**
     * Convenience overload accepting a UUID string.
     */
    public int toAgoraUid(String userId) {
        return toAgoraUid(UUID.fromString(userId));
    }
}
