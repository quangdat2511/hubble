package com.example.hubble.viewmodel.home;

import android.text.TextUtils;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.hubble.data.api.RetrofitClient;
import com.example.hubble.data.model.AuthResult;
import com.example.hubble.data.model.dm.ChannelDto;
import com.example.hubble.data.model.dm.FriendUserDto;
import com.example.hubble.data.model.dm.MessageDto;
import com.example.hubble.data.model.server.ServerItem;
import com.example.hubble.data.model.dm.DmConversationItem;
import com.example.hubble.data.repository.DmRepository;
import com.example.hubble.data.repository.ServerRepository;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.concurrent.atomic.AtomicInteger;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import ua.naiksoftware.stomp.Stomp;
import ua.naiksoftware.stomp.StompClient;
import ua.naiksoftware.stomp.dto.LifecycleEvent;

public class MainViewModel extends ViewModel {

    private final DmRepository dmRepository;
    private final ServerRepository serverRepository;
    private final String currentUserId;
    private final Gson gson = new Gson();
    private final CompositeDisposable dmRealtimeDisposables = new CompositeDisposable();
    private final Map<String, Disposable> dmTopicSubscriptions = new HashMap<>();
    private final Map<String, FriendUserDto> friendCacheById = new HashMap<>();
    private final Set<String> desiredDmChannelIds = new HashSet<>();

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

    private final MutableLiveData<AuthResult<List<ChannelDto>>> _serverChannels = new MutableLiveData<>();
    public final LiveData<AuthResult<List<ChannelDto>>> serverChannels = _serverChannels;

    private final Set<String> collapsedCategories = new HashSet<>();

    public MainViewModel(DmRepository dmRepository, ServerRepository serverRepository) {
        this.dmRepository = dmRepository;
        this.serverRepository = serverRepository;
        this.currentUserId = dmRepository.getCurrentUserId();
        _servers.setValue(new ArrayList<>());
        _selectedServer.setValue(null);
        refreshServers();
        refreshDirectMessages();
    }

    public void refreshServers() {
        serverRepository.getMyServers(result -> {
            if (result.getStatus() == AuthResult.Status.SUCCESS && result.getData() != null) {
                setServers(result.getData());
                return;
            }

            if (result.getStatus() == AuthResult.Status.ERROR) {
                _errorMessage.postValue(result.getMessage());
            }
        });
    }

    public void openOrCreateDirectChannel(String friendId) {
        if (friendId == null || friendId.trim().isEmpty()) {
            _openDmState.setValue(AuthResult.error("Có lỗi xảy ra. Vui lòng thử lại."));
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
        if (current != null) {
            for (ServerItem item : updated) {
                if (item.getId() != null && item.getId().equals(current.getId())) {
                    selectServer(item);
                    return;
                }
            }
        }

        selectServer(updated.get(0));
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

                Map<String, ChannelDto> channelByFriendId = new HashMap<>();
                for (ChannelDto channel : channelResult.getData()) {
                    if (channel == null || TextUtils.isEmpty(channel.getPeerUserId())) {
                        continue;
                    }
                    if (!friendById.containsKey(channel.getPeerUserId())) {
                        continue;
                    }
                    channelByFriendId.put(channel.getPeerUserId(), channel);
                }

                syncDmRealtimeChannels(new ArrayList<>(channelByFriendId.values()));

                List<DmConversationItem> conversations = new ArrayList<>();

                for (FriendUserDto friend : friends) {
                    if (friend == null || TextUtils.isEmpty(friend.getId())) {
                        continue;
                    }
                    conversations.add(buildConversationItem(friend, channelByFriendId.get(friend.getId())));
                }

                enrichWithLatestMessages(conversations, currentUserId);
            });
        });
    }

    private void upsertConversationForOpenedChannel(String friendId, ChannelDto channel) {
        if (TextUtils.isEmpty(friendId)) {
            return;
        }

        FriendUserDto friend = friendCacheById.get(friendId);
        DmConversationItem openedItem = buildConversationItem(friend, channel);

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
                "Direct Message"
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
                false
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
        dmRealtimeClient = Stomp.over(Stomp.ConnectionProvider.OKHTTP, wsUrl);
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

        dmRealtimeClient.connect();
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
                        // Keep app stable if one topic fails; refresh flow can re-sync subscriptions.
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
        if (current == null || current.isEmpty()) {
            return;
        }

        int existingIndex = -1;
        for (int i = 0; i < current.size(); i++) {
            DmConversationItem item = current.get(i);
            if (message.getChannelId().equals(item.getChannelId())) {
                existingIndex = i;
                break;
            }
        }

        if (existingIndex < 0) {
            return;
        }

        DmConversationItem currentItem = current.get(existingIndex);
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
                currentItem.isSelected()
        );

        List<DmConversationItem> next = new ArrayList<>(current);
        next.remove(existingIndex);
        next.add(0, updated);
        publishConversations(next);
    }

    private String buildRealtimePreview(MessageDto latest, String peerDisplayName) {
        if (Boolean.TRUE.equals(latest.getIsDeleted())) {
            String senderLabel = resolveSenderLabel(currentUserId, latest.getAuthorId(), peerDisplayName);
            return senderLabel + ": Tin nhắn đã được thu hồi";
        }

        String preview = latest.getContent();
        if (preview == null || preview.trim().isEmpty()) {
            preview = "Tin nhắn đa phương tiện";
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

    private void enrichWithLatestMessages(List<DmConversationItem> conversations, String currentUserId) {
        if (conversations.isEmpty()) {
            _dmConversations.postValue(conversations);
            _dmStories.postValue(List.of());
            return;
        }

        AtomicInteger pending = new AtomicInteger(0);
        List<DmConversationItem> enriched = new ArrayList<>(conversations);

        for (int i = 0; i < conversations.size(); i++) {
            DmConversationItem item = conversations.get(i);
            if (!item.hasChannelId()) {
                continue;
            }

            pending.incrementAndGet();
            final int index = i;
            dmRepository.getMessages(item.getChannelId(), 0, 1, result -> {
                if (result.getStatus() == AuthResult.Status.SUCCESS
                        && result.getData() != null
                        && !result.getData().isEmpty()) {
                    MessageDto latest = result.getData().get(0);
                    String preview = latest.getContent();
                    if (preview == null || preview.trim().isEmpty()) {
                        preview = "Tin nhắn đa phương tiện";
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
                    String senderLabel = resolveSenderLabel(currentUserId, latest.getAuthorId(), item.getDisplayName());
                    String previewText = senderLabel + ": " + preview;
                    synchronized (enriched) {
                        DmConversationItem current = enriched.get(index);
                        enriched.set(index, new DmConversationItem(
                                current.getId(), current.getChannelId(), current.getFriendId(),
                                current.getDisplayName(), previewText, toShortTime(latest.getCreatedAt()),
                                current.isOnline(), current.isVerified(), current.isSelected()
                        ));
                    }
                }
                if (pending.decrementAndGet() == 0) publishConversations(enriched);
            });
        }

        if (pending.get() == 0) {
            publishConversations(enriched);
        }
    }

    private void publishConversations(List<DmConversationItem> conversations) {
        _dmConversations.postValue(conversations);
        _dmStories.postValue(conversations.subList(0, Math.min(3, conversations.size())));
    }

    /**
     * Returns a Discord-style relative time label from an ISO timestamp.
     * e.g. "now", "5m", "2h", "3d", "1mo", "2y"
     * Handles both OffsetDateTime ("2024-01-01T10:00:00Z") and
     * LocalDateTime ("2024-01-01T10:00:00") server formats.
     */
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

            if (seconds < 60)        return "Bây giờ";
            long mins = seconds / 60;
            if (mins < 60)           return mins + " phút";
            long hours = mins / 60;
            if (hours < 24)          return hours + " giờ";
            long days = hours / 24;
            if (days < 30)           return days + " ngày";
            long months = days / 30;
            if (months < 12)         return months + " tháng";
            long years = days / 365;
            return years + " năm";
        } catch (Exception ignored) {
            return "";
        }
    }

    private String resolveSenderLabel(String currentUserId, String authorId, String peerDisplayName) {
        if (currentUserId != null && currentUserId.equalsIgnoreCase(authorId)) {
            return "Bạn";
        }

        if (peerDisplayName != null && !peerDisplayName.trim().isEmpty()) {
            return peerDisplayName;
        }

        return "Người dùng";
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
        return "Friend";
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
            _serverChannels.postValue(AuthResult.error("Máy chủ không hợp lệ"));
            return;
        }

        _serverChannels.postValue(AuthResult.loading());
        serverRepository.getServerChannels(serverId, result -> {
            if (result.getStatus() == AuthResult.Status.SUCCESS && result.getData() != null) {
                _serverChannels.postValue(AuthResult.success(result.getData()));
                return;
            }

            if (result.getStatus() == AuthResult.Status.ERROR) {
                _serverChannels.postValue(AuthResult.error(result.getMessage()));
            }
        });
    }

    public void toggleCategoryCollapse(String categoryId) {
        if (categoryId == null) {
            return;
        }

        if (collapsedCategories.contains(categoryId)) {
            collapsedCategories.remove(categoryId);
        } else {
            collapsedCategories.add(categoryId);
        }

        // Re-post current success data to trigger adapter rebuild
        AuthResult<List<ChannelDto>> current = _serverChannels.getValue();
        if (current != null && current.getStatus() == AuthResult.Status.SUCCESS && current.getData() != null) {
            _serverChannels.postValue(AuthResult.success(current.getData()));
        }
    }

    public Set<String> getCollapsedCategories() {
        return collapsedCategories;
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




