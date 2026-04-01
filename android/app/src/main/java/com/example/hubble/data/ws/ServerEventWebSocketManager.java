package com.example.hubble.data.ws;

import android.util.Log;

import com.example.hubble.data.model.server.ServerEventNotification;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;
import ua.naiksoftware.stomp.Stomp;
import ua.naiksoftware.stomp.StompClient;
import ua.naiksoftware.stomp.dto.StompHeader;

public class ServerEventWebSocketManager {

    private static final String TAG = "ServerEventWS";

    private static volatile ServerEventWebSocketManager instance;

    private StompClient stompClient;
    private CompositeDisposable disposables = new CompositeDisposable();

    private final PublishSubject<ServerEventNotification> eventSubject = PublishSubject.create();

    private String savedBaseUrl;
    private String savedUserId;
    private String savedToken;
    private volatile boolean reconnecting = false;

    private ServerEventWebSocketManager() {}

    public static ServerEventWebSocketManager getInstance() {
        if (instance == null) {
            synchronized (ServerEventWebSocketManager.class) {
                if (instance == null) {
                    instance = new ServerEventWebSocketManager();
                }
            }
        }
        return instance;
    }

    public Observable<ServerEventNotification> getEvents() {
        return eventSubject.hide();
    }

    public void connect(String baseUrl, String userId, String token) {
        savedBaseUrl = baseUrl;
        savedUserId  = userId;
        savedToken   = token;
        doConnect();
    }

    private void doConnect() {
        String baseUrl = savedBaseUrl;
        String userId  = savedUserId;
        String token   = savedToken;

        disconnect();

        savedBaseUrl = baseUrl;
        savedUserId  = userId;
        savedToken   = token;

        disposables = new CompositeDisposable();

        String wsUrl = savedBaseUrl
                .replace("https://", "wss://")
                .replace("http://",  "ws://");
        if (wsUrl.endsWith("/")) wsUrl = wsUrl.substring(0, wsUrl.length() - 1);
        wsUrl += "/ws";

        Log.d(TAG, "Connecting to " + wsUrl + " for user " + savedUserId);

        List<StompHeader> headers = new ArrayList<>();
        headers.add(new StompHeader("Authorization", "Bearer " + savedToken));

        stompClient = Stomp.over(Stomp.ConnectionProvider.OKHTTP, wsUrl);

        // Observe lifecycle events
        disposables.add(
            stompClient.lifecycle()
                .subscribeOn(Schedulers.io())
                .subscribe(event -> {
                    switch (event.getType()) {
                        case OPENED:
                            Log.d(TAG, "STOMP connected");
                            subscribeToUserTopic();
                            break;
                        case ERROR:
                            Log.e(TAG, "STOMP error: " + event.getException());
                            scheduleReconnect();
                            break;
                        case CLOSED:
                            Log.d(TAG, "STOMP closed");
                            scheduleReconnect();
                            break;
                        default:
                            break;
                    }
                }, t -> Log.e(TAG, "Lifecycle error", t))
        );

        stompClient.connect(headers);
    }

    private void subscribeToUserTopic() {
        String topic = "/topic/users/" + savedUserId + "/server-events";
        Log.d(TAG, "Subscribing to " + topic);

        disposables.add(
            stompClient.topic(topic)
                .subscribeOn(Schedulers.io())
                .subscribe(
                    stompMessage -> {
                        try {
                            JSONObject json = new JSONObject(stompMessage.getPayload());
                            String type       = json.optString("type", "");
                            String serverId   = json.optString("serverId", "");
                            String serverName = json.optString("serverName", "");
                            Log.d(TAG, "Event received: " + type + " / " + serverId);
                            eventSubject.onNext(new ServerEventNotification(type, serverId, serverName));
                        } catch (JSONException e) {
                            Log.e(TAG, "Failed to parse server event", e);
                        }
                    },
                    t -> Log.e(TAG, "Topic subscription error", t)
                )
        );
    }

    private void scheduleReconnect() {
        if (reconnecting || savedBaseUrl == null) return;
        reconnecting = true;
        Schedulers.io().scheduleDirect(() -> {
            try { Thread.sleep(5_000); } catch (InterruptedException ignored) {}
            reconnecting = false;
            if (savedBaseUrl != null) doConnect();
        });
    }

    public void disconnect() {
        reconnecting = false;
        savedBaseUrl = null; // prevent auto-reconnect after explicit disconnect
        disposables.clear();
        if (stompClient != null) {
            stompClient.disconnect();
            stompClient = null;
        }
    }

    public void updateToken(String newToken) {
        if (savedBaseUrl != null && savedUserId != null) {
            connect(savedBaseUrl, savedUserId, newToken);
        }
    }
}



