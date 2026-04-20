package com.example.hubble.viewmodel.home;

import android.content.Context;
import android.text.TextUtils;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.hubble.R;
import com.example.hubble.data.api.RetrofitClient;
import com.example.hubble.data.model.auth.AuthResult;
import com.example.hubble.data.model.dm.ChannelDto;
import com.example.hubble.data.model.dm.FriendUserDto;
import com.example.hubble.data.model.dm.MessageDto;
import com.example.hubble.data.realtime.ActiveDmChannelTracker;
import com.example.hubble.data.realtime.ActiveServerChannelTracker;
import com.example.hubble.data.model.server.ChannelEvent;
import com.example.hubble.data.model.server.ServerItem;
import com.example.hubble.data.model.dm.DmConversationItem;
import com.example.hubble.data.repository.DmRepository;
import com.example.hubble.data.repository.ServerRepository;
import com.google.gson.Gson;
import com.example.hubble.data.ws.ServerEventWebSocketManager;
import com.example.hubble.data.ws.FriendStatusEvent;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.TimeUnit;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import okhttp3.Dns;
import okhttp3.OkHttpClient;
import ua.naiksoftware.stomp.Stomp;
import ua.naiksoftware.stomp.StompClient;
import ua.naiksoftware.stomp.dto.LifecycleEvent;
import ua.naiksoftware.stomp.dto.StompHeader;

public class MainViewModel extends ViewModel {

    private static final String RAILWAY_HOST = "hubble-production.up.railway.app";
    private static final String[] RAILWAY_FALLBACK_IPS = {
            "151.101.2.15"
    };

    private final DmRepository dmRepository;
    private final ServerRepository serverRepository;
    private final Context appContext;
    private final String currentUserId;
    private final Gson gson = new Gson();
    private final CompositeDisposable dmRealtimeDisposables = new CompositeDisposable();
    private final Map<String, Disposable> dmTopicSubscriptions = new HashMap<>();
    private final Map<String, FriendUserDto> friendCacheById = new HashMap<>();
    private final Map<String, ChannelDto> channelCacheById = new HashMap<>();
    private final Set<String> desiredDmChannelIds = new HashSet<>();
    private final Set<String> favoriteChannelIds = new HashSet<>();

    private StompClient dmRealtimeClient;
    private boolean dmRealtimeConnected;
    private volatile boolean dmRealtimeReconnectPending;

    private final MutableLiveData<List<ServerItem>> _servers = new MutableLiveData<>();
    public final LiveData<List<ServerItem>> servers = _servers;

    private final MutableLiveData<ServerItem> _selectedServer = new MutableLiveData<>();
    public final LiveData<ServerItem> selectedServer = _selectedServer;

    private final MutableLiveData<List<DmConversationItem>> _dmStories = new MutableLiveData<>();
    public final LiveData<List<DmConversationItem>> dmStories = _dmStories;

    private final MutableLiveData<List<DmConversationItem>> _dmConversations = new MutableLiveData<>();
    public final LiveData<List<DmConversationItem>> dmConversations = _dmConversations;

    private final MutableLiveData<AuthResult<ChannelDto>> _openDmState = new MutableLiveData<>();
    public final LiveData<AuthResult<ChannelDto>> openDmState = _openDmState;

    private final MutableLiveData<String> _errorMessage = new MutableLiveData<>();
    public final LiveData<String> errorMessage = _errorMessage;

    private final MutableLiveData<String> _kickedFromServer = new MutableLiveData<>();
    public final LiveData<String> kickedFromServer = _kickedFromServer;

    private final MutableLiveData<AuthResult<List<ChannelDto>>> _serverChannels = new MutableLiveData<>();
    public final LiveData<AuthResult<List<ChannelDto>>> serverChannels = _serverChannels;

    private final MutableLiveData<Integer> _dmTotalUnread = new MutableLiveData<>(0);
    public final LiveData<Integer> dmTotalUnread = _dmTotalUnread;

    private final MutableLiveData<Map<String, Integer>> _serverUnreadByServerId = new MutableLiveData<>(new HashMap<>());
    public final LiveData<Map<String, Integer>> serverUnreadByServerId = _serverUnreadByServerId;

    /** Aggregate mention counts per server. Used to drive the red circular badge. */
    private final MutableLiveData<Map<String, Integer>> _serverMentionByServerId = new MutableLiveData<>(new HashMap<>());
    public final LiveData<Map<String, Integer>> serverMentionByServerId = _serverMentionByServerId;

    private final Set<String> collapsedCategories = new HashSet<>();
    private final Map<String, List<ChannelDto>> channelCache = new ConcurrentHashMap<>();
    private final CompositeDisposable wsDisposables = new CompositeDisposable();
    private volatile String activeServerId;

    // Server channel realtime
    private volatile String desiredServerChannelServerId = null;
    private volatile String subscribedServerChannelServerId = null;
    private Disposable serverChannelTopicDisposable = null;

    // Per-server-channel message topic subscriptions used to bump unread badges.
    private final Map<String, Disposable> serverChannelMessageSubscriptions = new HashMap<>();
    private final Set<String> desiredServerChannelIds = new HashSet<>();


    public MainViewModel(Context appContext, DmRepository dmRepository, ServerRepository serverRepository) {
        this.appContext = appContext.getApplicationContext();
        this.dmRepository = dmRepository;
        this.serverRepository = serverRepository;
        this.currentUserId = dmRepository.getCurrentUserId();
        _servers.setValue(new ArrayList<>());
        _selectedServer.setValue(null);
        observeServerEvents();
        observeServerChannelReadEvents();
        refreshServers();
        refreshDirectMessages();
    }

    private void observeServerChannelReadEvents() {
        wsDisposables.add(
                ActiveServerChannelTracker.observeChannelRead()
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(this::markServerChannelReadLocally, throwable -> {})
        );
    }

    private void observeServerEvents() {
        wsDisposables.add(
            ServerEventWebSocketManager.getInstance().getEvents()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(event -> {
                    switch (event.getType() != null ? event.getType() : "") {
                        case "KICKED":
                            removeServerById(event.getServerId());
                            _kickedFromServer.setValue(event.getServerName());
                            break;
                        case "SERVER_ADDED":
                        case "SERVER_JOINED":
                        case "SERVER_UPDATED":
                            refreshServers();
                            break;
                        case "SERVER_DELETED":
                            removeServerById(event.getServerId());
                            break;
                        default:
                            break;
                    }
                }, throwable -> {})
        );

        wsDisposables.add(
            ServerEventWebSocketManager.getInstance().getFriendStatusEvents()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::handleFriendStatusUpdate, throwable -> {})
        );
    }

    private void handleFriendStatusUpdate(FriendStatusEvent event) {
        String friendId = event.getUserId();
        if (friendId == null) return;

        // Update stories
        List<DmConversationItem> stories = _dmStories.getValue();
        if (stories != null) {
            List<DmConversationItem> updatedStories = new ArrayList<>();
            boolean storyChanged = false;
            for (DmConversationItem item : stories) {
                if (friendId.equals(item.getFriendId())) {
                    updatedStories.add(new DmConversationItem(
                            item.getId(), item.getChannelId(), item.getFriendId(),
                            item.getDisplayName(), item.getAvatarUrl(),
                            item.getLastMessage(), item.getTimeLabel(),
                            event.getStatus(), event.getCustomStatus(),
                            item.isVerified(), item.isSelected(), item.isFavorite(),
                            item.getUnreadCount(), item.getLastMessageAtMillis()));
                    storyChanged = true;
                } else {
                    updatedStories.add(item);
                }
            }
            if (storyChanged) _dmStories.setValue(updatedStories);
        }

        // Update conversations
        List<DmConversationItem> convos = _dmConversations.getValue();
        if (convos != null) {
            List<DmConversationItem> updatedConvos = new ArrayList<>();
            boolean convoChanged = false;
            for (DmConversationItem item : convos) {
                if (friendId.equals(item.getFriendId())) {
                    updatedConvos.add(new DmConversationItem(
                            item.getId(), item.getChannelId(), item.getFriendId(),
                            item.getDisplayName(), item.getAvatarUrl(),
                            item.getLastMessage(), item.getTimeLabel(),
                            event.getStatus(), event.getCustomStatus(),
                            item.isVerified(), item.isSelected(), item.isFavorite(),
                            item.getUnreadCount(), item.getLastMessageAtMillis()));
                    convoChanged = true;
                } else {
                    updatedConvos.add(item);
                }
            }
            if (convoChanged) _dmConversations.setValue(updatedConvos);
        }
    }

    public void removeServerById(String serverId) {
        List<ServerItem> current = _servers.getValue();
        if (current == null || serverId == null) return;
        List<ServerItem> updated = new ArrayList<>();
        for (ServerItem item : current) {
            if (!serverId.equals(item.getId())) updated.add(item);
        }
        setServers(updated);
    }

    public void consumeKickedFromServer() {
        _kickedFromServer.setValue(null);
    }

    // ─────────────────────────────────────────────────────────────────────

    public void refreshServers() {
        serverRepository.getMyServers(result -> {
            if (result.getStatus() == AuthResult.Status.SUCCESS && result.getData() != null) {
                setServers(result.getData());
                prefetchAllServerChannels(result.getData());
                return;
            }
            if (result.getStatus() == AuthResult.Status.ERROR) {
                _errorMessage.postValue(result.getMessage());
            }
        });
    }

    private void prefetchAllServerChannels(List<ServerItem> servers) {
        for (ServerItem server : servers) {
            String serverId = server.getId();
            if (serverId == null) continue;
            serverRepository.getServerChannels(serverId, result -> {
                if (result.getStatus() == AuthResult.Status.SUCCESS && result.getData() != null) {
                    channelCache.put(serverId, result.getData());
                    if (serverId.equals(activeServerId)) {
                        _serverChannels.postValue(AuthResult.success(result.getData()));
                    }
                    postServerUnreadSummaries();
                    syncServerChannelMessageSubscriptions();
                }
            });
        }
    }

    private void postServerUnreadSummaries() {
        List<ServerItem> serverList = _servers.getValue();
        if (serverList == null) {
            return;
        }
        Map<String, Integer> unreadMap = new HashMap<>();
        Map<String, Integer> mentionMap = new HashMap<>();
        for (ServerItem s : serverList) {
            if (s == null || TextUtils.isEmpty(s.getId())) continue;
            List<ChannelDto> chs = channelCache.get(s.getId());
            int unreadSum = 0;
            int mentionSum = 0;
            if (chs != null) {
                for (ChannelDto c : chs) {
                    if (c == null) continue;
                    Integer u = c.getUnreadCount();
                    if (u != null && u > 0) {
                        unreadSum += u;
                    }
                    Integer m = c.getMentionCount();
                    if (m != null && m > 0) {
                        mentionSum += m;
                    }
                }
            }
            if (unreadSum > 0) {
                unreadMap.put(s.getId(), Math.min(unreadSum, 9999));
            }
            if (mentionSum > 0) {
                mentionMap.put(s.getId(), Math.min(mentionSum, 9999));
            }
        }
        _serverUnreadByServerId.postValue(unreadMap);
        _serverMentionByServerId.postValue(mentionMap);
    }

    public void openOrCreateDirectChannel(String friendId) {
        if (friendId == null || friendId.trim().isEmpty()) {
            _openDmState.setValue(AuthResult.error(string(R.string.error_generic)));
            return;
        }

        _openDmState.setValue(AuthResult.loading());
        dmRepository.getOrCreateDirectChannel(friendId, result -> {
            if (result.getStatus() == AuthResult.Status.SUCCESS && result.getData() != null) {
                upsertConversationForOpenedChannel(friendId, result.getData());
            }
            _openDmState.postValue(result);
        });
    }

    public void consumeOpenDmState() {
        _openDmState.setValue(null);
    }

    public void consumeErrorMessage() {
        _errorMessage.setValue(null);
    }

    public void markConversationRead(String channelId, String friendId) {
        List<DmConversationItem> current = _dmConversations.getValue();
        if (current == null || current.isEmpty()) {
            return;
        }

        List<DmConversationItem> updated = new ArrayList<>(current.size());
        boolean changed = false;
        for (DmConversationItem item : current) {
            if (item == null) {
                continue;
            }
            boolean matched = (!TextUtils.isEmpty(channelId) && channelId.equals(item.getChannelId()))
                    || (!TextUtils.isEmpty(friendId) && friendId.equals(item.getFriendId()));
            if (matched && item.getUnreadCount() > 0) {
                updated.add(new DmConversationItem(
                        item.getId(),
                        item.getChannelId(),
                        item.getFriendId(),
                        item.getDisplayName(),
                        item.getAvatarUrl(),
                        item.getLastMessage(),
                        item.getTimeLabel(),
                        item.getStatus(),
                        item.getCustomStatus(),
                        item.isVerified(),
                        item.isSelected(),
                        item.isFavorite(),
                        0,
                        item.getLastMessageAtMillis()
                ));
                changed = true;
            } else {
                updated.add(item);
            }
        }

        if (changed) {
            publishConversations(updated);
        }
    }

    public void setServers(List<ServerItem> servers) {
        List<ServerItem> updated = servers != null ? new ArrayList<>(servers) : new ArrayList<>();
        _servers.setValue(updated);

        if (updated.isEmpty()) {
            _selectedServer.setValue(null);
            return;
        }

        ServerItem current = _selectedServer.getValue();
        if (current == null) {
            // User is on the DM panel; preserve that choice — do not auto-select any server.
            return;
        }

        // Re-select the same server with fresh data if it still exists.
        for (ServerItem item : updated) {
            if (item.getId() != null && item.getId().equals(current.getId())) {
                selectServer(item);
                return;
            }
        }

        // Previously-selected server was removed; fall back to the DM panel.
        _selectedServer.setValue(null);
    }

    public void selectServer(ServerItem server) {
        _selectedServer.setValue(server);
    }

    public void selectDmPanel() {
        _selectedServer.setValue(null);
    }

    public void refreshDirectMessages() {
        dmRepository.getFriends(friendResult -> {
            if (friendResult.getStatus() != AuthResult.Status.SUCCESS || friendResult.getData() == null) {
                _errorMessage.postValue(friendResult.getMessage());
                return;
            }

            List<FriendUserDto> friends = friendResult.getData();
            Map<String, FriendUserDto> friendById = new HashMap<>();
            friendCacheById.clear();
            for (FriendUserDto friend : friends) {
                if (friend == null || TextUtils.isEmpty(friend.getId())) {
                    continue;
                }
                friendById.put(friend.getId(), friend);
                friendCacheById.put(friend.getId(), friend);
            }

            dmRepository.getDirectChannels(channelResult -> {
                if (channelResult.getStatus() != AuthResult.Status.SUCCESS || channelResult.getData() == null) {
                    _errorMessage.postValue(channelResult.getMessage());
                    return;
                }

                Map<String, ChannelDto> channelByFriendId = new LinkedHashMap<>();
                channelCacheById.clear();
                for (ChannelDto channel : channelResult.getData()) {
                    if (channel == null || TextUtils.isEmpty(channel.getId()) || TextUtils.isEmpty(channel.getPeerUserId())) {
                        continue;
                    }
                    if (!friendById.containsKey(channel.getPeerUserId())) {
                        continue;
                    }
                    channelCacheById.put(channel.getId(), channel);
                    channelByFriendId.put(channel.getPeerUserId(), channel);
                }

                Set<String> availableChannelIds = new HashSet<>(channelCacheById.keySet());
                dmRepository.pruneLocallyOpenedDirectChannels(availableChannelIds);
                dmRepository.pruneFavoriteDirectChannels(availableChannelIds);
                Set<String> locallyOpenedChannelIds = dmRepository.getLocallyOpenedDirectChannelIds();
                favoriteChannelIds.clear();
                favoriteChannelIds.addAll(dmRepository.getFavoriteDirectChannelIds());

                syncDmRealtimeChannels(new ArrayList<>(channelByFriendId.values()));

                List<DmConversationItem> conversations = new ArrayList<>();

                for (ChannelDto channel : channelByFriendId.values()) {
                    FriendUserDto friend = friendById.get(channel.getPeerUserId());
                    conversations.add(buildConversationItem(friend, channel));
                }

                enrichWithLatestMessages(conversations, currentUserId, locallyOpenedChannelIds);
            });
        });
    }

    private void upsertConversationForOpenedChannel(String friendId, ChannelDto channel) {
        if (TextUtils.isEmpty(friendId)) {
            return;
        }

        if (channel != null && !TextUtils.isEmpty(channel.getId())) {
            channelCacheById.put(channel.getId(), channel);
            dmRepository.rememberOpenedDirectChannel(channel.getId());
        }

        FriendUserDto friend = friendCacheById.get(friendId);
        DmConversationItem baseItem = buildConversationItem(friend, channel);
        DmConversationItem openedItem = new DmConversationItem(
            baseItem.getId(),
            baseItem.getChannelId(),
            baseItem.getFriendId(),
            baseItem.getDisplayName(),
            baseItem.getAvatarUrl(),
            baseItem.getLastMessage(),
            baseItem.getTimeLabel(),
            baseItem.getStatus(),
            baseItem.getCustomStatus(),
            baseItem.isVerified(),
            baseItem.isSelected(),
            baseItem.isFavorite(),
            baseItem.getUnreadCount(),
            System.currentTimeMillis()
        );

        List<DmConversationItem> current = _dmConversations.getValue();
        List<DmConversationItem> next = current != null ? new ArrayList<>(current) : new ArrayList<>();
        for (int i = 0; i < next.size(); i++) {
            if (friendId.equals(next.get(i).getFriendId())) {
                next.remove(i);
                break;
            }
        }
        next.add(0, openedItem);
        publishConversations(next);

        if (channel != null && !TextUtils.isEmpty(channel.getId())) {
            desiredDmChannelIds.add(channel.getId());
            ensureDmRealtimeConnected();
            if (dmRealtimeConnected) {
                syncDmTopicSubscriptions();
            }
        }
    }

    private DmConversationItem buildConversationItem(FriendUserDto friend, ChannelDto channel) {
        String friendId = coalesce(
                friend != null ? friend.getId() : null,
                channel != null ? channel.getPeerUserId() : null,
                ""
        );
        String channelId = channel != null ? channel.getId() : null;

        String displayName = coalesce(
                channel != null ? channel.getPeerDisplayName() : null,
                channel != null ? channel.getPeerUsername() : null,
                friend != null ? displayNameOf(friend) : null,
                channel != null ? channel.getName() : null,
                string(R.string.dm_direct_message)
        );
        String peerStatus = coalesce(
                channel != null ? channel.getPeerStatus() : null,
                friend != null ? friend.getStatus() : null,
                ""
        );

        return new DmConversationItem(
                !TextUtils.isEmpty(channelId) ? channelId : "friend-" + friendId,
                channelId,
                friendId,
                displayName,
                coalesce(
                        friend != null ? friend.getAvatarUrl() : null,
                        channel != null ? channel.getPeerAvatarUrl() : null,
                        null
                ),
                "",
                "",
                peerStatus,
                friend != null ? friend.getCustomStatus() : null,
                false,
                false,
                isFavoriteChannel(channelId),
                unreadCountFromChannel(channel),
                0L
        );
    }

    private int unreadCountFromChannel(ChannelDto channel) {
        if (channel == null || channel.getUnreadCount() == null) {
            return 0;
        }
        int u = channel.getUnreadCount();
        if (u < 0) return 0;
        return Math.min(u, 999);
    }

    private void syncDmRealtimeChannels(List<ChannelDto> channels) {
        Set<String> channelIds = new HashSet<>();
        for (ChannelDto channel : channels) {
            if (channel != null && !TextUtils.isEmpty(channel.getId())) {
                channelIds.add(channel.getId());
            }
        }

        desiredDmChannelIds.clear();
        desiredDmChannelIds.addAll(channelIds);

        if (desiredDmChannelIds.isEmpty()) {
            clearDmTopicSubscriptions();
            return;
        }

        ensureDmRealtimeConnected();
        if (dmRealtimeConnected) {
            syncDmTopicSubscriptions();
        }
    }

    private void ensureDmRealtimeConnected() {
        if (dmRealtimeClient != null) {
            return;
        }

        String wsUrl = toWebSocketUrl(RetrofitClient.getBaseUrl()) + "ws";
        String accessToken = dmRepository.getAccessTokenRaw();
        Map<String, String> handshakeHeaders = new HashMap<>();
        List<StompHeader> connectHeaders = null;
        if (accessToken != null && !accessToken.trim().isEmpty()) {
            String authorization = "Bearer " + accessToken;
            handshakeHeaders.put("Authorization", authorization);
            connectHeaders = Collections.singletonList(new StompHeader("Authorization", authorization));
        }

        dmRealtimeClient = Stomp.over(
                Stomp.ConnectionProvider.OKHTTP,
                wsUrl,
                handshakeHeaders.isEmpty() ? null : handshakeHeaders,
                createRealtimeOkHttpClient()
        );
        dmRealtimeClient.withClientHeartbeat(10000).withServerHeartbeat(10000);

        final List<StompHeader> finalConnectHeaders = connectHeaders;
        dmRealtimeDisposables.add(dmRealtimeClient.lifecycle()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(event -> {
                    if (event.getType() == LifecycleEvent.Type.OPENED) {
                        dmRealtimeConnected = true;
                        syncDmTopicSubscriptions();
                        applyServerChannelMessageSubscriptions();
                        subscribeToServerChannelTopic(desiredServerChannelServerId);
                        // STOMP does not replay missed messages. Pull latest previews from REST
                        // so B sees new messages on the home list without opening the chat.
                        refreshDirectMessages();
                    } else if (event.getType() == LifecycleEvent.Type.CLOSED
                            || event.getType() == LifecycleEvent.Type.ERROR) {
                        dmRealtimeConnected = false;
                        subscribedServerChannelServerId = null;
                        serverChannelTopicDisposable = null;
                        clearDmTopicSubscriptions();
                        clearServerChannelMessageSubscriptions();
                        scheduleDmRealtimeReconnect();
                    }
                }, throwable -> {
                    dmRealtimeConnected = false;
                    scheduleDmRealtimeReconnect();
                }));

        dmRealtimeClient.connect(finalConnectHeaders);
    }

    private OkHttpClient createRealtimeOkHttpClient() {
        return new OkHttpClient.Builder()
                .dns(createDnsWithRailwayFallback())
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(0, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build();
    }

    private Dns createDnsWithRailwayFallback() {
        return hostname -> {
            try {
                return Dns.SYSTEM.lookup(hostname);
            } catch (UnknownHostException originalError) {
                if (!RAILWAY_HOST.equalsIgnoreCase(hostname)) {
                    throw originalError;
                }

                List<InetAddress> fallbackAddresses = new ArrayList<>();
                for (String ip : RAILWAY_FALLBACK_IPS) {
                    try {
                        fallbackAddresses.add(InetAddress.getByName(ip));
                    } catch (UnknownHostException ignored) {
                        // Ignore malformed fallback entries.
                    }
                }

                if (fallbackAddresses.isEmpty()) {
                    throw originalError;
                }
                return fallbackAddresses;
            }
        };
    }

    /**
     * After REST sync, tell the server to broadcast delivery acks so senders see "✓✓ Đã nhận"
     * without the recipient opening DmChatActivity.
     */
    private void sendDeliveryAcksForAllDesiredChannels() {
        if (!dmRealtimeConnected || dmRealtimeClient == null || TextUtils.isEmpty(currentUserId)) {
            return;
        }
        String payload = "{\"userId\":\"" + currentUserId + "\"}";
        for (String channelId : new HashSet<>(desiredDmChannelIds)) {
            if (TextUtils.isEmpty(channelId)) {
                continue;
            }
            final String cid = channelId;
            dmRealtimeDisposables.add(
                    dmRealtimeClient.send("/app/channels/" + cid + "/delivered", payload)
                            .subscribeOn(Schedulers.io())
                            .subscribe(() -> {}, throwable -> {
                            })
            );
        }
    }

    private void scheduleDmRealtimeReconnect() {
        if (dmRealtimeReconnectPending || desiredDmChannelIds.isEmpty()) {
            return;
        }
        dmRealtimeReconnectPending = true;
        Schedulers.io().scheduleDirect(() -> {
            try {
                Thread.sleep(3_000);
            } catch (InterruptedException ignored) {
            }
            AndroidSchedulers.mainThread().scheduleDirect(() -> {
                dmRealtimeReconnectPending = false;
                dmRealtimeDisposables.clear();
                if (dmRealtimeClient != null) {
                    try {
                        dmRealtimeClient.disconnect();
                    } catch (Exception ignored) {
                    }
                    dmRealtimeClient = null;
                }
                dmRealtimeConnected = false;
                if (!desiredDmChannelIds.isEmpty()) {
                    ensureDmRealtimeConnected();
                }
            });
        });
    }

    private void syncDmTopicSubscriptions() {
        if (!dmRealtimeConnected || dmRealtimeClient == null) {
            return;
        }

        List<String> currentSubscribed = new ArrayList<>(dmTopicSubscriptions.keySet());
        for (String channelId : currentSubscribed) {
            if (!desiredDmChannelIds.contains(channelId)) {
                Disposable disposable = dmTopicSubscriptions.remove(channelId);
                if (disposable != null && !disposable.isDisposed()) {
                    disposable.dispose();
                }
            }
        }

        for (String channelId : desiredDmChannelIds) {
            if (dmTopicSubscriptions.containsKey(channelId)) {
                continue;
            }

            final String subscribedChannelId = channelId;
            Disposable topicDisposable = dmRealtimeClient
                    .topic("/topic/channels/" + subscribedChannelId)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(stompMessage -> {
                        MessageDto dto = gson.fromJson(stompMessage.getPayload(), MessageDto.class);
                        if (dto == null) {
                            return;
                        }
                        if (TextUtils.isEmpty(dto.getChannelId())) {
                            dto.setChannelId(subscribedChannelId);
                        }
                        upsertConversationFromRealtime(dto);
                    }, throwable -> {
                    });

            dmTopicSubscriptions.put(subscribedChannelId, topicDisposable);
            dmRealtimeDisposables.add(topicDisposable);
        }
    }

    private void clearDmTopicSubscriptions() {
        for (Disposable disposable : dmTopicSubscriptions.values()) {
            if (disposable != null && !disposable.isDisposed()) {
                disposable.dispose();
            }
        }
        dmTopicSubscriptions.clear();
    }

    private String toWebSocketUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.trim().isEmpty()) {
            return "ws://";
        }

        String normalized = baseUrl.endsWith("/")
                ? baseUrl.substring(0, baseUrl.length() - 1)
                : baseUrl;
        String lower = normalized.toLowerCase();

        if (lower.startsWith("https://")) {
            return "wss://" + normalized.substring("https://".length()) + "/";
        }
        if (lower.startsWith("http://")) {
            return "ws://" + normalized.substring("http://".length()) + "/";
        }
        return "ws://" + normalized + "/";
    }

    private void upsertConversationFromRealtime(MessageDto message) {
        if (message == null || TextUtils.isEmpty(message.getChannelId())) {
            return;
        }

        List<DmConversationItem> current = _dmConversations.getValue();
        List<DmConversationItem> currentList = current != null ? current : new ArrayList<>();

        int existingIndex = -1;
        for (int i = 0; i < currentList.size(); i++) {
            DmConversationItem item = currentList.get(i);
            if (message.getChannelId().equals(item.getChannelId())) {
                existingIndex = i;
                break;
            }
        }

        if (existingIndex < 0) {
            DmConversationItem seededItem = buildConversationFromChannelId(message.getChannelId());
            if (seededItem == null) {
                return;
            }

            String previewText = buildRealtimePreview(message, seededItem.getDisplayName());
            String timeLabel = toShortTime(message.getCreatedAt());
            if (timeLabel == null || timeLabel.trim().isEmpty()) {
                timeLabel = seededItem.getTimeLabel();
            }

            String activeChannelId = ActiveDmChannelTracker.getActiveChannelId();
            boolean isIncomingFromPeer = currentUserId == null || !currentUserId.equals(message.getAuthorId());
            boolean isCurrentlyOpened = !TextUtils.isEmpty(activeChannelId)
                    && activeChannelId.equals(message.getChannelId());
            int seedUnread = (isIncomingFromPeer && !isCurrentlyOpened) ? 1 : 0;
            DmConversationItem inserted = new DmConversationItem(
                    seededItem.getId(),
                    seededItem.getChannelId(),
                    seededItem.getFriendId(),
                    seededItem.getDisplayName(),
                    seededItem.getAvatarUrl(),
                    previewText,
                    timeLabel,
                    seededItem.getStatus(),
                    seededItem.getCustomStatus(),
                    seededItem.isVerified(),
                        seededItem.isSelected(),
                        seededItem.isFavorite(),
                        seedUnread,
                        toEpochMillis(message.getCreatedAt(), seededItem.getLastMessageAtMillis())
            );

            List<DmConversationItem> next = new ArrayList<>(currentList);
            next.add(0, inserted);
            publishConversations(next);
            return;
        }

        DmConversationItem currentItem = currentList.get(existingIndex);
        String previewText = buildRealtimePreview(message, currentItem.getDisplayName());
        String timeLabel = toShortTime(message.getCreatedAt());
        if (timeLabel == null || timeLabel.trim().isEmpty()) {
            timeLabel = currentItem.getTimeLabel();
        }

        String activeChannelId = ActiveDmChannelTracker.getActiveChannelId();
        boolean isIncomingFromPeer = currentUserId == null || !currentUserId.equals(message.getAuthorId());
        boolean isCurrentlyOpened = !TextUtils.isEmpty(activeChannelId)
                && activeChannelId.equals(message.getChannelId());
        int nextUnread = currentItem.getUnreadCount();
        if (isIncomingFromPeer && !isCurrentlyOpened) {
            nextUnread = Math.min(999, nextUnread + 1);
        } else if (isCurrentlyOpened) {
            nextUnread = 0;
        }
        DmConversationItem updated = new DmConversationItem(
                currentItem.getId(),
                currentItem.getChannelId(),
                currentItem.getFriendId(),
                currentItem.getDisplayName(),
                currentItem.getAvatarUrl(),
                previewText,
                timeLabel,
                currentItem.getStatus(),
                currentItem.getCustomStatus(),
                currentItem.isVerified(),
                currentItem.isSelected(),
                currentItem.isFavorite(),
                nextUnread,
                toEpochMillis(message.getCreatedAt(), currentItem.getLastMessageAtMillis())
        );

        List<DmConversationItem> next = new ArrayList<>(currentList);
        next.remove(existingIndex);
        next.add(0, updated);
        publishConversations(next);
    }

    private DmConversationItem buildConversationFromChannelId(String channelId) {
        if (TextUtils.isEmpty(channelId)) {
            return null;
        }

        ChannelDto channel = channelCacheById.get(channelId);
        if (channel == null) {
            return null;
        }

        FriendUserDto friend = null;
        if (!TextUtils.isEmpty(channel.getPeerUserId())) {
            friend = friendCacheById.get(channel.getPeerUserId());
        }
        return buildConversationItem(friend, channel);
    }

    private String buildRealtimePreview(MessageDto latest, String peerDisplayName) {
        if (Boolean.TRUE.equals(latest.getIsDeleted())) {
            String senderLabel = resolveSenderLabel(currentUserId, latest.getAuthorId(), peerDisplayName);
            return senderLabel + ": " + string(R.string.dm_deleted_message);
        }

        String preview = latest.getContent();
        if (preview == null || preview.trim().isEmpty()) {
            preview = string(R.string.dm_reply_media);
        } else if (preview.startsWith("{gif}")) {
            String body = preview.substring(5);
            int nl = body.indexOf('\n');
            String title = nl > 0 ? body.substring(0, nl).trim() : null;
            preview = (title != null && !title.isEmpty() ? title : "GIF") + " 🎬";
        } else if (preview.startsWith("{sticker}")) {
            String body = preview.substring(9);
            int nl = body.indexOf('\n');
            String title = nl > 0 ? body.substring(0, nl).trim() : null;
            preview = (title != null && !title.isEmpty() ? title : "Sticker") + " 🎭";
        }

        String senderLabel = resolveSenderLabel(currentUserId, latest.getAuthorId(), peerDisplayName);
        return senderLabel + ": " + preview;
    }

    private void enrichWithLatestMessages(
            List<DmConversationItem> conversations,
            String currentUserId,
            Set<String> locallyOpenedChannelIds
    ) {
        if (conversations.isEmpty()) {
            _dmConversations.postValue(conversations);
            _dmStories.postValue(List.of());
            _dmTotalUnread.postValue(0);
            sendDeliveryAcksForAllDesiredChannels();
            return;
        }

        AtomicInteger pending = new AtomicInteger(conversations.size());
        List<DmConversationItem> enriched = new ArrayList<>(conversations.size());
        for (int i = 0; i < conversations.size(); i++) {
            enriched.add(null);
        }

        for (int i = 0; i < conversations.size(); i++) {
            DmConversationItem item = conversations.get(i);
            final int index = i;
            dmRepository.getMessages(item.getChannelId(), 0, 1, result -> {
                DmConversationItem resolvedItem = null;
                if (result.getStatus() == AuthResult.Status.SUCCESS
                        && result.getData() != null
                        && !result.getData().isEmpty()) {
                    MessageDto latest = result.getData().get(0);
                    String previewText = buildRealtimePreview(latest, item.getDisplayName());
                    String timeLabel = toShortTime(latest.getCreatedAt());
                    if (timeLabel == null || timeLabel.trim().isEmpty()) {
                        timeLabel = item.getTimeLabel();
                    }

                    resolvedItem = new DmConversationItem(
                            item.getId(),
                            item.getChannelId(),
                            item.getFriendId(),
                            item.getDisplayName(),
                            item.getAvatarUrl(),
                            previewText,
                            timeLabel,
                            item.getStatus(),
                            item.getCustomStatus(),
                            item.isVerified(),
                                item.isSelected(),
                                item.isFavorite(),
                                item.getUnreadCount(),
                                toEpochMillis(latest.getCreatedAt(), item.getLastMessageAtMillis())
                    );
                } else {
                    boolean showOpenedByCurrentUser = item.hasChannelId()
                            && locallyOpenedChannelIds != null
                            && locallyOpenedChannelIds.contains(item.getChannelId());
                    boolean keepIfMessageLookupFailed = result.getStatus() != AuthResult.Status.SUCCESS;
                    if (showOpenedByCurrentUser || keepIfMessageLookupFailed) {
                        resolvedItem = item;
                    }
                }

                synchronized (enriched) {
                    enriched.set(index, resolvedItem);
                }

                if (pending.decrementAndGet() == 0) {
                    List<DmConversationItem> visibleConversations = new ArrayList<>();
                    synchronized (enriched) {
                        for (DmConversationItem candidate : enriched) {
                            if (candidate != null) {
                                visibleConversations.add(candidate);
                            }
                        }
                    }
                    publishConversations(visibleConversations);
                    sendDeliveryAcksForAllDesiredChannels();
                }
            });
        }
    }

    private void publishConversations(List<DmConversationItem> conversations) {
        List<DmConversationItem> sorted = sortConversationsByPriority(conversations);
        _dmConversations.postValue(sorted);
        _dmStories.postValue(sorted.subList(0, Math.min(3, sorted.size())));
        int sum = 0;
        for (DmConversationItem i : sorted) {
            if (i != null) {
                sum += i.getUnreadCount();
            }
        }
        _dmTotalUnread.postValue(Math.max(0, sum));
    }

    public void toggleConversationFavorite(DmConversationItem conversation) {
        if (conversation == null || TextUtils.isEmpty(conversation.getChannelId())) {
            return;
        }

        boolean shouldFavorite = !conversation.isFavorite();
        dmRepository.setDirectChannelFavorite(conversation.getChannelId(), shouldFavorite);
        if (shouldFavorite) {
            favoriteChannelIds.add(conversation.getChannelId());
        } else {
            favoriteChannelIds.remove(conversation.getChannelId());
        }

        List<DmConversationItem> current = _dmConversations.getValue();
        if (current == null || current.isEmpty()) {
            return;
        }

        List<DmConversationItem> updated = new ArrayList<>(current.size());
        for (DmConversationItem item : current) {
            if (item != null && conversation.getChannelId().equals(item.getChannelId())) {
                updated.add(new DmConversationItem(
                        item.getId(),
                        item.getChannelId(),
                        item.getFriendId(),
                        item.getDisplayName(),
                        item.getAvatarUrl(),
                        item.getLastMessage(),
                        item.getTimeLabel(),
                        item.getStatus(),
                        item.getCustomStatus(),
                        item.isVerified(),
                        item.isSelected(),
                        shouldFavorite,
                        item.getUnreadCount(),
                        item.getLastMessageAtMillis()
                ));
            } else {
                updated.add(item);
            }
        }

        publishConversations(updated);
    }

    private List<DmConversationItem> sortConversationsByPriority(List<DmConversationItem> conversations) {
        List<DmConversationItem> sorted = conversations != null ? new ArrayList<>(conversations) : new ArrayList<>();
        sorted.sort((left, right) -> {
            if (left == right) {
                return 0;
            }
            if (left == null) {
                return 1;
            }
            if (right == null) {
                return -1;
            }
            if (left.isFavorite() != right.isFavorite()) {
                return left.isFavorite() ? -1 : 1;
            }

            int byLatestMessage = Long.compare(right.getLastMessageAtMillis(), left.getLastMessageAtMillis());
            if (byLatestMessage != 0) {
                return byLatestMessage;
            }

            String leftName = left.getDisplayName() != null ? left.getDisplayName() : "";
            String rightName = right.getDisplayName() != null ? right.getDisplayName() : "";
            return leftName.compareToIgnoreCase(rightName);
        });
        return sorted;
    }

    private boolean isFavoriteChannel(String channelId) {
        if (TextUtils.isEmpty(channelId)) {
            return false;
        }
        return favoriteChannelIds.contains(channelId);
    }

    private long toEpochMillis(String createdAt, long fallback) {
        if (createdAt == null || createdAt.trim().isEmpty()) {
            return fallback;
        }

        try {
            try {
                return OffsetDateTime.parse(createdAt).toInstant().toEpochMilli();
            } catch (DateTimeParseException e) {
                return LocalDateTime.parse(createdAt)
                        .atZone(ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli();
            }
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private String toShortTime(String createdAt) {
        if (createdAt == null || createdAt.trim().isEmpty()) return "";
        try {
            Instant then;
            try {
                then = OffsetDateTime.parse(createdAt).toInstant();
            } catch (DateTimeParseException e) {
                then = LocalDateTime.parse(createdAt)
                        .atZone(ZoneId.systemDefault())
                        .toInstant();
            }

            long seconds = Duration.between(then, Instant.now()).getSeconds();
            if (seconds < 0) seconds = 0;

            if (seconds < 60)        return string(R.string.time_now);
            long mins = seconds / 60;
            if (mins < 60)           return string(R.string.time_minutes, mins);
            long hours = mins / 60;
            if (hours < 24)          return string(R.string.time_hours, hours);
            long days = hours / 24;
            if (days < 30)           return string(R.string.time_days, days);
            long months = days / 30;
            if (months < 12)         return string(R.string.time_months, months);
            long years = days / 365;
            return string(R.string.time_years, years);
        } catch (Exception ignored) {
            return "";
        }
    }

    private String resolveSenderLabel(String currentUserId, String authorId, String peerDisplayName) {
        if (currentUserId != null && currentUserId.equalsIgnoreCase(authorId)) {
            return string(R.string.main_you_sender);
        }

        if (peerDisplayName != null && !peerDisplayName.trim().isEmpty()) {
            return peerDisplayName;
        }

        return string(R.string.main_unknown_user);
    }

    private String displayNameOf(FriendUserDto friend) {
        String displayName = friend.getDisplayName();
        if (displayName != null && !displayName.trim().isEmpty()) {
            return displayName;
        }
        String username = friend.getUsername();
        if (username != null && !username.trim().isEmpty()) {
            return username;
        }
        return string(R.string.main_friend_fallback);
    }

    private String coalesce(String... values) {
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value;
            }
        }
        return null;
    }

    public void loadServerChannels(String serverId) {
        if (serverId == null || serverId.trim().isEmpty()) {
            _serverChannels.postValue(AuthResult.error(string(R.string.invalid_server_error)));
            return;
        }

        activeServerId = serverId;

        List<ChannelDto> cached = channelCache.get(serverId);
        if (cached != null) {
            _serverChannels.postValue(AuthResult.success(cached));
        }

        serverRepository.getServerChannels(serverId, result -> {
            if (result.getStatus() == AuthResult.Status.SUCCESS && result.getData() != null) {
                channelCache.put(serverId, result.getData());
                if (serverId.equals(activeServerId)) {
                    _serverChannels.postValue(AuthResult.success(result.getData()));
                }
                postServerUnreadSummaries();
                syncServerChannelMessageSubscriptions();
                return;
            }
            if (result.getStatus() == AuthResult.Status.ERROR && cached == null
                    && serverId.equals(activeServerId)) {
                _serverChannels.postValue(AuthResult.error(result.getMessage()));
            }
        });

        ensureDmRealtimeConnected();
        subscribeToServerChannelTopic(serverId);
    }

    /**
     * Public hook used by UI right before navigating into a server text channel:
     * optimistically zero its unread count and tell the backend we've read through
     * the latest message id we know about.
     */
    public void markServerChannelRead(String channelId) {
        if (TextUtils.isEmpty(channelId)) {
            return;
        }
        markServerChannelReadLocally(channelId);
        dmRepository.markChannelRead(channelId, null, result -> {});
    }

    /**
     * Find the channel across every cached server, reset its unreadCount to 0,
     * and broadcast the updated channel list + aggregate summary.
     */
    private void markServerChannelReadLocally(String channelId) {
        if (TextUtils.isEmpty(channelId)) {
            return;
        }
        boolean changed = false;
        for (Map.Entry<String, List<ChannelDto>> entry : channelCache.entrySet()) {
            List<ChannelDto> list = entry.getValue();
            if (list == null) continue;
            for (int i = 0; i < list.size(); i++) {
                ChannelDto c = list.get(i);
                if (c == null || !channelId.equals(c.getId())) continue;

                boolean hadUnread = c.getUnreadCount() != null && c.getUnreadCount() > 0;
                boolean hadMention = c.getMentionCount() != null && c.getMentionCount() > 0;
                if (!hadUnread && !hadMention) continue;

                c.setUnreadCount(0);
                c.setMentionCount(0);
                changed = true;
                if (entry.getKey() != null && entry.getKey().equals(activeServerId)) {
                    _serverChannels.postValue(AuthResult.success(new ArrayList<>(list)));
                }
            }
        }
        if (changed) {
            postServerUnreadSummaries();
        }
    }

    /**
     * Ensure we have a STOMP subscription for every text channel across every
     * cached server so incoming messages can bump per-channel unread counts even
     * when the user isn't viewing that server.
     */
    private void syncServerChannelMessageSubscriptions() {
        Set<String> desired = new HashSet<>();
        for (List<ChannelDto> list : channelCache.values()) {
            if (list == null) continue;
            for (ChannelDto c : list) {
                if (c == null || TextUtils.isEmpty(c.getId())) continue;
                if (!"TEXT".equalsIgnoreCase(c.getType())) continue;
                desired.add(c.getId());
            }
        }
        desiredServerChannelIds.clear();
        desiredServerChannelIds.addAll(desired);

        if (desiredServerChannelIds.isEmpty()) {
            clearServerChannelMessageSubscriptions();
            return;
        }

        ensureDmRealtimeConnected();
        if (dmRealtimeConnected) {
            applyServerChannelMessageSubscriptions();
        }
    }

    private void applyServerChannelMessageSubscriptions() {
        if (!dmRealtimeConnected || dmRealtimeClient == null) {
            return;
        }
        List<String> current = new ArrayList<>(serverChannelMessageSubscriptions.keySet());
        for (String channelId : current) {
            if (!desiredServerChannelIds.contains(channelId)) {
                Disposable d = serverChannelMessageSubscriptions.remove(channelId);
                if (d != null && !d.isDisposed()) d.dispose();
            }
        }

        for (String channelId : desiredServerChannelIds) {
            if (serverChannelMessageSubscriptions.containsKey(channelId)) continue;
            if (desiredDmChannelIds.contains(channelId)) continue; // DM subscription already handles it

            final String cid = channelId;
            Disposable d = dmRealtimeClient
                    .topic("/topic/channels/" + cid)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(stompMessage -> {
                        MessageDto dto = parseServerChannelMessage(stompMessage.getPayload());
                        if (dto == null) return;
                        if (TextUtils.isEmpty(dto.getChannelId())) {
                            dto.setChannelId(cid);
                        }
                        onIncomingServerChannelMessage(dto);
                    }, throwable -> {});
            serverChannelMessageSubscriptions.put(cid, d);
            dmRealtimeDisposables.add(d);
        }
    }

    private void clearServerChannelMessageSubscriptions() {
        for (Disposable d : serverChannelMessageSubscriptions.values()) {
            if (d != null && !d.isDisposed()) d.dispose();
        }
        serverChannelMessageSubscriptions.clear();
    }

    private MessageDto parseServerChannelMessage(String payload) {
        if (payload == null) return null;
        try {
            MessageDto direct = gson.fromJson(payload, MessageDto.class);
            if (direct != null && !TextUtils.isEmpty(direct.getChannelId())) {
                return direct;
            }
        } catch (Exception ignored) {}
        return null;
    }

    /**
     * Bump the unreadCount for the channel the incoming message belongs to,
     * unless it was authored by us or the user is currently viewing that channel.
     */
    private void onIncomingServerChannelMessage(MessageDto message) {
        if (message == null || TextUtils.isEmpty(message.getChannelId())) return;

        boolean authoredBySelf = currentUserId != null
                && currentUserId.equals(message.getAuthorId());
        String active = ActiveServerChannelTracker.getActiveChannelId();
        boolean isCurrentlyOpened = !TextUtils.isEmpty(active)
                && active.equals(message.getChannelId());
        if (authoredBySelf || isCurrentlyOpened) {
            return;
        }

        boolean changed = false;
        String ownerServerId = null;
        List<ChannelDto> ownerList = null;
        for (Map.Entry<String, List<ChannelDto>> entry : channelCache.entrySet()) {
            List<ChannelDto> list = entry.getValue();
            if (list == null) continue;
            for (ChannelDto c : list) {
                if (c != null && message.getChannelId().equals(c.getId())) {
                    int u = c.getUnreadCount() != null ? c.getUnreadCount() : 0;
                    c.setUnreadCount(Math.min(9999, u + 1));
                    ownerServerId = entry.getKey();
                    ownerList = list;
                    changed = true;
                    break;
                }
            }
            if (changed) break;
        }
        if (!changed) return;

        if (ownerServerId != null && ownerServerId.equals(activeServerId) && ownerList != null) {
            _serverChannels.postValue(AuthResult.success(new ArrayList<>(ownerList)));
        }
        postServerUnreadSummaries();
    }

    private void subscribeToServerChannelTopic(String serverId) {
        desiredServerChannelServerId = serverId;
        if (!dmRealtimeConnected || dmRealtimeClient == null || serverId == null) return;
        if (serverId.equals(subscribedServerChannelServerId)) return;

        if (serverChannelTopicDisposable != null && !serverChannelTopicDisposable.isDisposed()) {
            serverChannelTopicDisposable.dispose();
        }
        subscribedServerChannelServerId = serverId;

        final String sid = serverId;
        serverChannelTopicDisposable = dmRealtimeClient
                .topic("/topic/servers/" + sid + "/channels")
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(stompMessage -> {
                    ChannelEvent event = gson.fromJson(stompMessage.getPayload(), ChannelEvent.class);
                    if (event == null || event.getChannel() == null) return;
                    ChannelDto channel = event.getChannel();
                    if (TextUtils.isEmpty(channel.getId())) return;
                    String type = event.getType();
                    if ("DELETED".equals(type)) {
                        onServerChannelDeleted(sid, channel.getId());
                    } else if ("UPDATED".equals(type)) {
                        onServerChannelUpdated(sid, channel);
                    } else {
                        onServerChannelCreated(sid, channel);
                    }
                }, throwable -> {});
        dmRealtimeDisposables.add(serverChannelTopicDisposable);
    }

    private void onServerChannelCreated(String serverId, ChannelDto newChannel) {
        if (Boolean.TRUE.equals(newChannel.getIsPrivate())) {
            // For private channels, re-fetch so access control is applied by the server
            loadServerChannels(serverId);
            return;
        }
        List<ChannelDto> current = channelCache.get(serverId);
        List<ChannelDto> updated = current != null ? new ArrayList<>(current) : new ArrayList<>();
        for (ChannelDto c : updated) {
            if (c.getId() != null && c.getId().equals(newChannel.getId())) return;
        }
        updated.add(newChannel);
        channelCache.put(serverId, updated);
        if (serverId.equals(activeServerId)) {
            _serverChannels.postValue(AuthResult.success(updated));
        }
        syncServerChannelMessageSubscriptions();
    }

    private void onServerChannelUpdated(String serverId, ChannelDto updatedChannel) {
        if (Boolean.TRUE.equals(updatedChannel.getIsPrivate())) {
            // For private channels, re-fetch so access control is recalculated
            loadServerChannels(serverId);
            return;
        }
        List<ChannelDto> current = channelCache.get(serverId);
        if (current == null) return;
        List<ChannelDto> updated = new ArrayList<>(current);
        boolean found = false;
        for (int i = 0; i < updated.size(); i++) {
            if (updated.get(i).getId() != null && updated.get(i).getId().equals(updatedChannel.getId())) {
                updated.set(i, updatedChannel);
                found = true;
                break;
            }
        }
        if (!found) {
            updated.add(updatedChannel);
        }
        channelCache.put(serverId, updated);
        if (serverId.equals(activeServerId)) {
            _serverChannels.postValue(AuthResult.success(updated));
        }
    }

    private void onServerChannelDeleted(String serverId, String channelId) {
        List<ChannelDto> current = channelCache.get(serverId);
        if (current == null) return;
        List<ChannelDto> updated = new ArrayList<>();
        for (ChannelDto c : current) {
            if (!channelId.equals(c.getId())) {
                updated.add(c);
            }
        }
        channelCache.put(serverId, updated);
        if (serverId.equals(activeServerId)) {
            _serverChannels.postValue(AuthResult.success(updated));
        }
        postServerUnreadSummaries();
        syncServerChannelMessageSubscriptions();
    }

    public void toggleCategoryCollapse(String categoryId) {
        if (categoryId == null) return;
        if (collapsedCategories.contains(categoryId)) {
            collapsedCategories.remove(categoryId);
        } else {
            collapsedCategories.add(categoryId);
        }

        AuthResult<List<ChannelDto>> current = _serverChannels.getValue();
        if (current != null && current.getStatus() == AuthResult.Status.SUCCESS && current.getData() != null) {
            _serverChannels.postValue(AuthResult.success(current.getData()));
        }
    }

    public Set<String> getCollapsedCategories() {
        return collapsedCategories;
    }

    private String string(int resId, Object... args) {
        return appContext.getString(resId, args);
    }

    @Override
    protected void onCleared() {
        dmRealtimeReconnectPending = false;
        clearDmTopicSubscriptions();
        clearServerChannelMessageSubscriptions();
        if (serverChannelTopicDisposable != null && !serverChannelTopicDisposable.isDisposed()) {
            serverChannelTopicDisposable.dispose();
        }
        wsDisposables.clear();
        dmRealtimeDisposables.clear();
        if (dmRealtimeClient != null) {
            dmRealtimeClient.disconnect();
            dmRealtimeClient = null;
        }
        dmRealtimeConnected = false;
        super.onCleared();
    }
}
