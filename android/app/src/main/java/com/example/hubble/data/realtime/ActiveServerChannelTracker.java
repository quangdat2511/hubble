package com.example.hubble.data.realtime;

import androidx.annotation.Nullable;

import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;

/**
 * Tracks the server text channel currently open on screen and broadcasts
 * "channel read" events from DmChatActivity back to any interested listener
 * (typically MainViewModel) so it can zero out local unreadCount state.
 *
 * <p>Server text channels live in {@link com.example.hubble.view.dm.DmChatActivity}
 * (the same screen used for DMs), which runs as its own Android activity. This
 * holder lets a running {@link androidx.lifecycle.ViewModel} in the parent
 * activity (HomeFragment / MainActivity) observe read-events from the chat
 * screen without introducing a circular dependency.</p>
 */
public final class ActiveServerChannelTracker {

    private static final AtomicReference<String> ACTIVE_SERVER_CHANNEL_ID = new AtomicReference<>(null);
    private static final PublishSubject<String> READ_EVENTS = PublishSubject.create();

    private ActiveServerChannelTracker() {
    }

    public static void setActiveChannelId(@Nullable String channelId) {
        ACTIVE_SERVER_CHANNEL_ID.set(channelId);
    }

    @Nullable
    public static String getActiveChannelId() {
        return ACTIVE_SERVER_CHANNEL_ID.get();
    }

    public static void clearIfMatch(@Nullable String channelId) {
        if (channelId == null) {
            return;
        }
        ACTIVE_SERVER_CHANNEL_ID.compareAndSet(channelId, null);
    }

    /** Emit a "channel was marked read" signal for listeners (MainViewModel). */
    public static void notifyChannelRead(@Nullable String channelId) {
        if (channelId == null || channelId.isEmpty()) {
            return;
        }
        READ_EVENTS.onNext(channelId);
    }

    /** Observe channel-read events (emits channelId strings). */
    public static Observable<String> observeChannelRead() {
        return READ_EVENTS;
    }
}
