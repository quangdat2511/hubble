package com.example.hubble.data.realtime;

import androidx.annotation.Nullable;

import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;

/**
 * Tracks the DM channel currently open on screen and broadcasts "channel
 * read" events from {@link com.example.hubble.view.dm.DmChatActivity} back to
 * any interested listener (typically {@code MainViewModel}) so it can zero
 * out local unread state without waiting for the next REST refresh.
 */
public final class ActiveDmChannelTracker {

    private static final AtomicReference<String> ACTIVE_DM_CHANNEL_ID = new AtomicReference<>(null);
    private static final PublishSubject<String> READ_EVENTS = PublishSubject.create();

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

    /** Emit a "channel was marked read" signal for listeners (MainViewModel). */
    public static void notifyChannelRead(@Nullable String channelId) {
        if (channelId == null || channelId.isEmpty()) {
            return;
        }
        READ_EVENTS.onNext(channelId);
    }

    /** Observe DM channel-read events (emits channelId strings). */
    public static Observable<String> observeChannelRead() {
        return READ_EVENTS;
    }
}
