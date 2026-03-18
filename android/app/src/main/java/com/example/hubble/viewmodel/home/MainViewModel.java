package com.example.hubble.viewmodel.home;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.hubble.data.model.AuthResult;
import com.example.hubble.data.model.dm.ChannelDto;
import com.example.hubble.data.model.dm.FriendUserDto;
import com.example.hubble.data.model.dm.MessageDto;
import com.example.hubble.data.model.server.ServerItem;
import com.example.hubble.data.model.dm.DmConversationItem;
import com.example.hubble.data.repository.DmRepository;
import com.example.hubble.data.repository.ServerRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.concurrent.atomic.AtomicInteger;

public class MainViewModel extends ViewModel {

    private final DmRepository dmRepository;
    private final ServerRepository serverRepository;

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

    public MainViewModel(DmRepository dmRepository, ServerRepository serverRepository) {
        this.dmRepository = dmRepository;
        this.serverRepository = serverRepository;
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
        dmRepository.getOrCreateDirectChannel(friendId, _openDmState::postValue);
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

    public void refreshDirectMessages() {
        dmRepository.getFriends(friendResult -> {
            if (friendResult.getStatus() != AuthResult.Status.SUCCESS || friendResult.getData() == null) {
                _errorMessage.postValue(friendResult.getMessage());
                return;
            }

            dmRepository.getDirectChannels(channelResult -> {
                if (channelResult.getStatus() != AuthResult.Status.SUCCESS || channelResult.getData() == null) {
                    _errorMessage.postValue(channelResult.getMessage());
                    return;
                }

                List<FriendUserDto> friends = friendResult.getData();
                Map<String, FriendUserDto> friendById = new HashMap<>();
                for (FriendUserDto friend : friends) {
                    friendById.put(friend.getId(), friend);
                }

                List<DmConversationItem> conversations = new ArrayList<>();
                for (ChannelDto channel : channelResult.getData()) {
                    FriendUserDto matchedFriend = null;
                    if (channel.getPeerUserId() != null) {
                        matchedFriend = friendById.get(channel.getPeerUserId());
                    }

                    String peerName = coalesce(
                            channel.getPeerDisplayName(),
                            channel.getPeerUsername(),
                            channel.getName(),
                            matchedFriend != null ? displayNameOf(matchedFriend) : null,
                            "Direct Message"
                    );
                    String preview = "Chưa có tin nhắn";
                    String peerStatus = coalesce(
                            channel.getPeerStatus(),
                            matchedFriend != null ? matchedFriend.getStatus() : null,
                            ""
                    );
                    boolean online = "ONLINE".equalsIgnoreCase(peerStatus);
                    boolean verified = false;

                    String peerUserId = coalesce(
                            channel.getPeerUserId(),
                            matchedFriend != null ? matchedFriend.getId() : null,
                            null
                    );

                    conversations.add(new DmConversationItem(
                            channel.getId(),
                            channel.getId(),
                            peerUserId,
                            peerName,
                            preview,
                            "now",
                            online,
                            verified,
                            false
                    ));
                }

                if (conversations.isEmpty() && !friends.isEmpty()) {
                    for (FriendUserDto friend : friends) {
                        conversations.add(new DmConversationItem(
                                "friend-" + friend.getId(),
                                null,
                                friend.getId(),
                                displayNameOf(friend),
                                "Bắt đầu cuộc trò chuyện",
                                "",
                                "ONLINE".equalsIgnoreCase(friend.getStatus()),
                                false,
                                false
                        ));
                    }
                }

                enrichWithLatestMessages(conversations, dmRepository.getCurrentUserId());
            });
        });
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
                    }

                    String senderLabel = resolveSenderLabel(currentUserId, latest.getAuthorId(), item.getDisplayName());
                    String previewText = senderLabel + ": " + preview;

                    synchronized (enriched) {
                        DmConversationItem current = enriched.get(index);
                        enriched.set(index, new DmConversationItem(
                                current.getId(),
                                current.getChannelId(),
                                current.getFriendId(),
                                current.getDisplayName(),
                                previewText,
                                toShortTime(latest.getCreatedAt()),
                                current.isOnline(),
                                current.isVerified(),
                                current.isSelected()
                        ));
                    }
                }

                if (pending.decrementAndGet() == 0) {
                    publishConversations(enriched);
                }
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

    private String toShortTime(String createdAt) {
        if (createdAt == null || createdAt.trim().isEmpty()) {
            return "";
        }

        try {
            OffsetDateTime dateTime = OffsetDateTime.parse(createdAt);
            int hour = dateTime.getHour();
            int minute = dateTime.getMinute();
            return String.format(Locale.getDefault(), "%02d:%02d", hour, minute);
        } catch (DateTimeParseException ignored) {
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
}




