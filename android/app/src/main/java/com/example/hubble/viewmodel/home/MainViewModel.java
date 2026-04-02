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
import com.example.hubble.data.model.server.ServerItem;
import com.example.hubble.data.model.dm.DmConversationItem;
import com.example.hubble.data.repository.DmRepository;
import com.example.hubble.data.repository.ServerRepository;
import com.google.gson.Gson;
import com.example.hubble.data.ws.ServerEventWebSocketManager;

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

    private final Set<String> collapsedCategories = new HashSet<>();
    private final Map<String, List<ChannelDto>> channelCache = new ConcurrentHashMap<>();
    private final CompositeDisposable wsDisposables = new CompositeDisposable();
    private volatile String activeServerId;

    public MainViewModel(Context appContext, DmRepository dmRepository, ServerRepository serverRepository) {
        this.appContext = appContext.getApplicationContext();
        this.dmRepository = dmRepository;
        this.serverRepository = serverRepository;
        this.currentUserId = dmRepository.getCurrentUserId();
        _servers.setValue(new ArrayList<>());
        _selectedServer.setValue(null);
        observeServerEvents();
        refreshServers();
        refreshDirectMessages();
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
                }
            });
        }
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
            baseItem.getLastMessage(),
            baseItem.getTimeLabel(),
            baseItem.isOnline(),
            baseItem.isVerified(),
            baseItem.isSelected(),
            baseItem.isFavorite(),
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
                "",
                "",
                "ONLINE".equalsIgnoreCase(peerStatus),
                false,
                false,
                isFavoriteChannel(channelId),
                0L
        );
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
                    } else if (event.getType() == LifecycleEvent.Type.CLOSED
                            || event.getType() == LifecycleEvent.Type.ERROR) {
                        dmRealtimeConnected = false;
                        clearDmTopicSubscriptions();
                    }
                }, throwable -> {
                    dmRealtimeConnected = false;
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

            DmConversationItem inserted = new DmConversationItem(
                    seededItem.getId(),
                    seededItem.getChannelId(),
                    seededItem.getFriendId(),
                    seededItem.getDisplayName(),
                    previewText,
                    timeLabel,
                    seededItem.isOnline(),
                    seededItem.isVerified(),
                        seededItem.isSelected(),
                        seededItem.isFavorite(),
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

        DmConversationItem updated = new DmConversationItem(
                currentItem.getId(),
                currentItem.getChannelId(),
                currentItem.getFriendId(),
                currentItem.getDisplayName(),
                previewText,
                timeLabel,
                currentItem.isOnline(),
                currentItem.isVerified(),
                currentItem.isSelected(),
                currentItem.isFavorite(),
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
                            previewText,
                            timeLabel,
                            item.isOnline(),
                            item.isVerified(),
                                item.isSelected(),
                                item.isFavorite(),
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
                }
            });
        }
    }

    private void publishConversations(List<DmConversationItem> conversations) {
        List<DmConversationItem> sorted = sortConversationsByPriority(conversations);
        _dmConversations.postValue(sorted);
        _dmStories.postValue(sorted.subList(0, Math.min(3, sorted.size())));
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
                        item.getLastMessage(),
                        item.getTimeLabel(),
                        item.isOnline(),
                        item.isVerified(),
                        item.isSelected(),
                        shouldFavorite,
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
                return;
            }
            if (result.getStatus() == AuthResult.Status.ERROR && cached == null
                    && serverId.equals(activeServerId)) {
                _serverChannels.postValue(AuthResult.error(result.getMessage()));
            }
        });
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
        clearDmTopicSubscriptions();
        dmRealtimeDisposables.clear();
        if (dmRealtimeClient != null) {
            dmRealtimeClient.disconnect();
            dmRealtimeClient = null;
        }
        dmRealtimeConnected = false;
        super.onCleared();
    }
}
