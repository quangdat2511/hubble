package com.example.hubble.data.ws;

import android.util.Log;

import com.example.hubble.data.model.notify.NotificationResponse;
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

    // Emits {channelId} whenever a DM message is delivered to this user's device.
    // DmChatActivity observes this to mark the sender's messages as DELIVERED.
    private final PublishSubject<String> dmDeliverySubject = PublishSubject.create();
    private final PublishSubject<NotificationResponse> notificationSubject = PublishSubject.create();
    private final PublishSubject<FriendStatusEvent> friendStatusSubject = PublishSubject.create();

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

    /**
     * Emits the channelId every time a DM message arrives on THIS user's device
     * (even when not in the chat screen).  The sender will see "✓✓ Đã nhận".
     */
    public Observable<String> getDmDeliveryEvents() {
        return dmDeliverySubject.hide();
    }

    public Observable<NotificationResponse> getNotificationEvents() {
        return notificationSubject.hide();
    }

    public Observable<FriendStatusEvent> getFriendStatusEvents() {
        return friendStatusSubject.hide();
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
        subscribeToServerEvents();
        subscribeToDmDelivery();
        subscribeToNotifications();
        subscribeToFriendStatus();
    }

    private void subscribeToServerEvents() {
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
                            String roleId     = json.optString("roleId", null);
                            Log.d(TAG, "Event received: " + type + " / " + serverId);
                            eventSubject.onNext(new ServerEventNotification(type, serverId, serverName, roleId));
                        } catch (JSONException e) {
                            Log.e(TAG, "Failed to parse server event", e);
                        }
                    },
                    t -> Log.e(TAG, "Topic subscription error", t)
                )
        );
    }

    /**
     * Subscribes to DM delivery events pushed by the server when a DM message
     * is saved.  When a delivery event arrives, we send an ack back to the
     * channel's delivery topic — so the SENDER sees "✓✓ Đã nhận" as soon as
     * this user's device receives the message, regardless of which screen they
     * are on.
     */
    private void subscribeToDmDelivery() {
        String topic = "/topic/users/" + savedUserId + "/dm-delivery";
        Log.d(TAG, "Subscribing to DM delivery topic: " + topic);

        disposables.add(
            stompClient.topic(topic)
                .subscribeOn(Schedulers.io())
                .subscribe(
                    stompMessage -> {
                        try {
                            JSONObject json = new JSONObject(stompMessage.getPayload());
                            String channelId = json.optString("channelId", "");
                            String senderId  = json.optString("senderId", "");
                            if (channelId.isEmpty() || senderId.isEmpty()) return;

                            Log.d(TAG, "DM delivery ack for channel " + channelId);

                            // Emit to any observer (e.g. DmChatActivity)
                            dmDeliverySubject.onNext(channelId);

                            // Also send the ack back via STOMP so the sender's
                            // app marks the message DELIVERED immediately.
                            String ackPayload = "{\"userId\":\"" + savedUserId + "\"}";
                            disposables.add(
                                stompClient.send(
                                        "/app/channels/" + channelId + "/delivered",
                                        ackPayload
                                )
                                .subscribeOn(Schedulers.io())
                                .subscribe(() -> {}, t -> Log.e(TAG, "Delivery ack send error", t))
                            );
                        } catch (JSONException e) {
                            Log.e(TAG, "Failed to parse dm-delivery event", e);
                        }
                    },
                    t -> Log.e(TAG, "DM delivery subscription error", t)
                )
        );
    }

    private void subscribeToNotifications() {
        String topic = "/topic/users/" + savedUserId + "/notifications";
        Log.d(TAG, "Subscribing to notifications topic: " + topic);

        disposables.add(
            stompClient.topic(topic)
                .subscribeOn(Schedulers.io())
                .subscribe(
                    stompMessage -> {
                        try {
                            com.google.gson.Gson gson = new com.google.gson.Gson();
                            NotificationResponse notification = gson.fromJson(
                                    stompMessage.getPayload(), NotificationResponse.class);
                            if (notification != null) {
                                Log.d(TAG, "Notification received: " + notification.getType());
                                notificationSubject.onNext(notification);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Failed to parse notification", e);
                        }
                    },
                    t -> Log.e(TAG, "Notification subscription error", t)
                )
        );
    }

    private void subscribeToFriendStatus() {
        String topic = "/topic/users/" + savedUserId + "/friend-status";
        Log.d(TAG, "Subscribing to friend-status topic: " + topic);

        disposables.add(
            stompClient.topic(topic)
                .subscribeOn(Schedulers.io())
                .subscribe(
                    stompMessage -> {
                        try {
                            JSONObject json = new JSONObject(stompMessage.getPayload());
                            String userId = json.optString("userId", "");
                            String status = json.optString("status", "");
                            String customStatus = json.isNull("customStatus")
                                    ? null : json.optString("customStatus", null);
                            String lastSeenAt = json.isNull("lastSeenAt")
                                    ? null : json.optString("lastSeenAt", null);
                            if (!userId.isEmpty()) {
                                Log.d(TAG, "Friend status update: " + userId + " -> " + status);
                                friendStatusSubject.onNext(
                                        new FriendStatusEvent(userId, status, customStatus, lastSeenAt));
                            }
                        } catch (JSONException e) {
                            Log.e(TAG, "Failed to parse friend-status event", e);
                        }
                    },
                    t -> Log.e(TAG, "Friend status subscription error", t)
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



