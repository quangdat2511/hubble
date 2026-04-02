package com.example.hubble.data.realtime;

import android.content.Context;

import com.example.hubble.R;
import com.example.hubble.data.api.RetrofitClient;
import com.example.hubble.data.model.dm.MessageDto;
import com.example.hubble.utils.TokenManager;
import com.google.gson.Gson;

import java.util.Collections;
import java.util.Locale;

import io.reactivex.disposables.Disposable;
import ua.naiksoftware.stomp.Stomp;
import ua.naiksoftware.stomp.StompClient;
import ua.naiksoftware.stomp.dto.StompHeader;

public class DmStompClient {

    public interface Listener {
        void onMessage(MessageDto message);
        void onError(String message);
    }

    private final TokenManager tokenManager;
    private final Gson gson;
    private final Context appContext;

    private StompClient stompClient;
    private Disposable topicDisposable;

    public DmStompClient(Context context) {
        this.appContext = context.getApplicationContext();
        this.tokenManager = new TokenManager(appContext);
        this.gson = new Gson();
    }

    public void connectAndSubscribe(String channelId, Listener listener) {
        disconnect();

        String accessToken = tokenManager.getAccessToken();
        if (accessToken == null || accessToken.trim().isEmpty()) {
            listener.onError(appContext.getString(R.string.realtime_not_logged_in));
            return;
        }

        String wsUrl = toWebSocketUrl(RetrofitClient.getBaseUrl()) + "ws";
        String destination = "/topic/channel/" + channelId;

        stompClient = Stomp.over(Stomp.ConnectionProvider.OKHTTP, wsUrl);
        stompClient.withClientHeartbeat(10000).withServerHeartbeat(10000);
        stompClient.connect();

        StompHeader authHeader = new StompHeader("Authorization", "Bearer " + accessToken);
        topicDisposable = stompClient.topic(destination, Collections.singletonList(authHeader))
                .subscribe(topicMessage -> {
                    try {
                        MessageEventPayload payload = gson.fromJson(topicMessage.getPayload(), MessageEventPayload.class);
                        if (payload == null || payload.message == null) {
                            return;
                        }

                        if (payload.action == null || payload.action.trim().isEmpty()
                                || "SEND".equalsIgnoreCase(payload.action)
                                || "EDIT".equalsIgnoreCase(payload.action)
                                || "DELETE".equalsIgnoreCase(payload.action)) {
                            listener.onMessage(payload.message);
                        }
                    } catch (Exception e) {
                        listener.onError(appContext.getString(R.string.realtime_parse_error));
                    }
                }, throwable -> listener.onError(appContext.getString(
                        R.string.realtime_disconnected,
                        throwable.getMessage()
                )));
    }

    public void disconnect() {
        if (topicDisposable != null && !topicDisposable.isDisposed()) {
            topicDisposable.dispose();
            topicDisposable = null;
        }
        if (stompClient != null) {
            stompClient.disconnect();
            stompClient = null;
        }
    }

    private String toWebSocketUrl(String baseUrl) {
        String normalized = baseUrl;
        if (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }

        String lower = normalized.toLowerCase(Locale.ROOT);
        if (lower.startsWith("https://")) {
            return "wss://" + normalized.substring("https://".length()) + "/";
        }
        if (lower.startsWith("http://")) {
            return "ws://" + normalized.substring("http://".length()) + "/";
        }
        return "ws://" + normalized + "/";
    }

    private static class MessageEventPayload {
        String action;
        MessageDto message;
    }
}
