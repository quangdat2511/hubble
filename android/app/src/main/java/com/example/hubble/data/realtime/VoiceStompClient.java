package com.example.hubble.data.realtime;

import android.util.Log;

import com.example.hubble.data.api.NetworkConfig;
import com.example.hubble.data.model.voice.VoiceSignalMessage;
import com.example.hubble.utils.TokenManager;
import com.google.gson.Gson;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import okhttp3.OkHttpClient;
import ua.naiksoftware.stomp.Stomp;
import ua.naiksoftware.stomp.StompClient;
import ua.naiksoftware.stomp.dto.LifecycleEvent;
import ua.naiksoftware.stomp.dto.StompHeader;

public class VoiceStompClient {
    private static final String TAG = "VoiceStompClient";

    public interface Listener {
        void onSignalMessage(VoiceSignalMessage message);
        void onError(String message);
        void onConnected();
    }

    private final TokenManager tokenManager;
    private final Gson gson;

    private StompClient stompClient;
    private final CompositeDisposable disposables = new CompositeDisposable();
    private String currentChannelId;
    private String currentUserId;
    private boolean connected;

    public VoiceStompClient(TokenManager tokenManager) {
        this.tokenManager = tokenManager;
        this.gson = new Gson();
    }

    public void connect(String channelId, String userId, Listener listener) {
        disconnect();
        this.currentChannelId = channelId;
        this.currentUserId = userId;

        String accessToken = tokenManager.getAccessToken();
        if (accessToken == null || accessToken.trim().isEmpty()) {
            listener.onError("Not logged in");
            return;
        }

        String authorization = "Bearer " + accessToken;
        String wsUrl = NetworkConfig.getWebSocketUrl("ws");

        // HTTP handshake headers (same pattern as the working MainViewModel STOMP client)
        Map<String, String> handshakeHeaders = new HashMap<>();
        handshakeHeaders.put("Authorization", authorization);

        // STOMP CONNECT frame headers
        List<StompHeader> connectHeaders = Collections.singletonList(
                new StompHeader("Authorization", authorization));

        // Auth header for SUBSCRIBE frames
        StompHeader authHeader = new StompHeader("Authorization", authorization);

        // Use a custom OkHttpClient with infinite read timeout for long-lived connection
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(0, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build();

        stompClient = Stomp.over(
                Stomp.ConnectionProvider.OKHTTP, wsUrl, handshakeHeaders, okHttpClient);
        stompClient.withClientHeartbeat(10000).withServerHeartbeat(10000);

        // Lifecycle: when OPENED, set up subscriptions then notify listener
        disposables.add(stompClient.lifecycle().subscribe(event -> {
            switch (event.getType()) {
                case OPENED:
                    Log.d(TAG, "STOMP connected to " + wsUrl);
                    connected = true;
                    subscribeToTopics(channelId, userId, authHeader, listener);
                    listener.onConnected();
                    break;
                case ERROR:
                    Log.e(TAG, "STOMP error", event.getException());
                    connected = false;
                    listener.onError("Connection error");
                    break;
                case CLOSED:
                    Log.d(TAG, "STOMP closed");
                    connected = false;
                    break;
            }
        }));

        // Connect with auth headers (matching the proven MainViewModel pattern)
        Log.d(TAG, "Connecting to " + wsUrl);
        stompClient.connect(connectHeaders);
    }

    private void subscribeToTopics(String channelId, String userId,
                                    StompHeader authHeader, Listener listener) {
        // Broadcast channel (join/leave events)
        disposables.add(stompClient.topic(
                "/topic/voice/" + channelId,
                Collections.singletonList(authHeader)
        ).subscribe(msg -> {
            try {
                VoiceSignalMessage signal = gson.fromJson(msg.getPayload(), VoiceSignalMessage.class);
                if (signal != null) {
                    listener.onSignalMessage(signal);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error parsing broadcast message", e);
            }
        }, throwable -> {
            Log.e(TAG, "Broadcast subscription error", throwable);
        }));

        // User-specific channel (offers/answers/ICE)
        disposables.add(stompClient.topic(
                "/topic/voice/" + channelId + "/user/" + userId,
                Collections.singletonList(authHeader)
        ).subscribe(msg -> {
            try {
                VoiceSignalMessage signal = gson.fromJson(msg.getPayload(), VoiceSignalMessage.class);
                if (signal != null) {
                    listener.onSignalMessage(signal);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error parsing user message", e);
            }
        }, throwable -> {
            Log.e(TAG, "User subscription error", throwable);
        }));

        Log.d(TAG, "Subscribed to voice topics for channel=" + channelId + " user=" + userId);
    }

    public void sendJoin(String userId, String displayName, String avatarUrl) {
        VoiceSignalMessage msg = new VoiceSignalMessage();
        msg.setType("join");
        msg.setChannelId(currentChannelId);
        msg.setUserId(userId);
        msg.setDisplayName(displayName);
        msg.setAvatarUrl(avatarUrl);
        send("/app/voice/" + currentChannelId + "/join", msg);
    }

    public void sendLeave(String userId) {
        VoiceSignalMessage msg = new VoiceSignalMessage();
        msg.setType("leave");
        msg.setChannelId(currentChannelId);
        msg.setUserId(userId);
        send("/app/voice/" + currentChannelId + "/leave", msg);
    }

    public void sendOffer(String userId, String targetUserId, String sdp) {
        VoiceSignalMessage msg = new VoiceSignalMessage();
        msg.setType("offer");
        msg.setChannelId(currentChannelId);
        msg.setUserId(userId);
        msg.setTargetUserId(targetUserId);
        msg.setSdp(sdp);
        send("/app/voice/" + currentChannelId + "/offer", msg);
    }

    public void sendAnswer(String userId, String targetUserId, String sdp) {
        VoiceSignalMessage msg = new VoiceSignalMessage();
        msg.setType("answer");
        msg.setChannelId(currentChannelId);
        msg.setUserId(userId);
        msg.setTargetUserId(targetUserId);
        msg.setSdp(sdp);
        send("/app/voice/" + currentChannelId + "/answer", msg);
    }

    public void sendIceCandidate(String userId, String targetUserId,
                                  String candidate, String sdpMid, int sdpMLineIndex) {
        VoiceSignalMessage msg = new VoiceSignalMessage();
        msg.setType("ice-candidate");
        msg.setChannelId(currentChannelId);
        msg.setUserId(userId);
        msg.setTargetUserId(targetUserId);
        msg.setCandidate(candidate);
        msg.setSdpMid(sdpMid);
        msg.setSdpMLineIndex(sdpMLineIndex);
        send("/app/voice/" + currentChannelId + "/ice-candidate", msg);
    }

    private void send(String destination, VoiceSignalMessage msg) {
        if (stompClient == null) {
            Log.w(TAG, "Cannot send " + msg.getType() + ": stompClient is null");
            return;
        }
        if (!connected) {
            Log.w(TAG, "Cannot send " + msg.getType() + ": not connected");
            return;
        }
        Log.d(TAG, "Sending " + msg.getType() + " to " + destination
                + (msg.getTargetUserId() != null ? " target=" + msg.getTargetUserId() : ""));
        Disposable d = stompClient.send(destination, gson.toJson(msg))
                .timeout(5, TimeUnit.SECONDS)
                .subscribe(
                        () -> Log.d(TAG, "Sent OK: " + msg.getType()),
                        e -> Log.e(TAG, "Send FAILED for " + msg.getType(), e)
                );
        disposables.add(d);
    }

    public void disconnect() {
        disposables.clear();
        if (stompClient != null) {
            stompClient.disconnect();
            stompClient = null;
        }
        currentChannelId = null;
    }

    public boolean isConnected() {
        return stompClient != null && stompClient.isConnected();
    }

}
