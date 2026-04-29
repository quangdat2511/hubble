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
    private static final PublishSubject<ServerChannelReadEvent> READ_EVENTS = PublishSubject.create();

    /** Payload for "channel was marked read" events. */
    public static final class ServerChannelReadEvent {
        private final String channelId;
        /**
         * Boundary createdAtMillis used by the chat UI when sending a read receipt.
         * If negative, listeners should treat it as "unknown boundary" and apply
         * their normal local-optimistic behavior.
         */
        private final long boundaryCreatedAtMillis;

        public ServerChannelReadEvent(String channelId, long boundaryCreatedAtMillis) {
            this.channelId = channelId;
            this.boundaryCreatedAtMillis = boundaryCreatedAtMillis;
        }

        public String getChannelId() {
            return channelId;
        }

        public long getBoundaryCreatedAtMillis() {
            return boundaryCreatedAtMillis;
        }
    }

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
        notifyChannelRead(channelId, -1L);
    }

    /**
     * Emit a "channel was marked read" signal for listeners (MainViewModel).
     *
     * @param boundaryCreatedAtMillis createdAtMillis of the latest message included
     *                                 in the read receipt. -1 means unknown.
     */
    public static void notifyChannelRead(@Nullable String channelId, long boundaryCreatedAtMillis) {
        if (channelId == null || channelId.isEmpty()) {
            return;
        }
        READ_EVENTS.onNext(new ServerChannelReadEvent(channelId, boundaryCreatedAtMillis));
    }

    /** Observe channel-read events (emits channelId strings). */
    public static Observable<String> observeChannelRead() {
        // Backward-compat overload kept intentionally: callers that depend on the
        // older "String-only" API can still subscribe without having to change.
        return READ_EVENTS.map(ServerChannelReadEvent::getChannelId);
    }

    /** Observe channel-read events (emits read boundary payload). */
    public static Observable<ServerChannelReadEvent> observeChannelReadEvents() {
        return READ_EVENTS;
    }
}
