package com.example.hubble.utils;

import java.util.UUID;

/**
 * Maps UUID user IDs to Agora uint32 UIDs deterministically.
 *
 * Strategy: XOR the two 64-bit halves of the UUID, then mask to 31 bits.
 * UID 0 is reserved by Agora and is remapped to 1.
 *
 * Must stay in sync with the backend AgoraUidMapper.
 */
public class AgoraUidMapper {

    private AgoraUidMapper() {}

    public static int toAgoraUid(String userId) {
        UUID uuid = UUID.fromString(userId);
        long hash = uuid.getMostSignificantBits() ^ uuid.getLeastSignificantBits();
        int uid = (int) (hash & 0x7FFFFFFFL);
        return uid == 0 ? 1 : uid;
    }
}
