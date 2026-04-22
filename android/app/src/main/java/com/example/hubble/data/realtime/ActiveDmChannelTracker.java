package com.example.hubble.data.realtime;

import androidx.annotation.Nullable;

import java.util.concurrent.atomic.AtomicReference;

public final class ActiveDmChannelTracker {

    private static final AtomicReference<String> ACTIVE_DM_CHANNEL_ID = new AtomicReference<>(null);

    private ActiveDmChannelTracker() {
    }

    public static void setActiveChannelId(@Nullable String channelId) {
        ACTIVE_DM_CHANNEL_ID.set(channelId);
    }

    @Nullable
    public static String getActiveChannelId() {
        return ACTIVE_DM_CHANNEL_ID.get();
    }

    public static void clearIfMatch(@Nullable String channelId) {
        if (channelId == null) {
            return;
        }
        ACTIVE_DM_CHANNEL_ID.compareAndSet(channelId, null);
    }
}
