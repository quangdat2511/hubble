package com.hubble.entity;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Cờ {@code isPinned} trên {@link ChannelMember} hỗ trợ ghim hội thoại ở tầng dữ liệu.
 * (Chưa có REST riêng trong codebase — kiểm thử mô hình để tránh hồi quy schema.)
 */
class ChannelMemberPinConversationTest {

    @Test
    void newMember_defaultsPinnedFalse() {
        ChannelMember m = ChannelMember.builder()
                .channelId(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .build();
        assertFalse(Boolean.TRUE.equals(m.getIsPinned()));
    }

    @Test
    void pinnedFlag_canBeSetTrue() {
        ChannelMember m = ChannelMember.builder()
                .channelId(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .isPinned(true)
                .build();
        assertTrue(Boolean.TRUE.equals(m.getIsPinned()));
    }
}
