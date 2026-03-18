package com.example.hubble.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.hubble.data.model.AuthResult;
import com.example.hubble.data.model.ChannelDto;
import com.example.hubble.data.model.ChannelListItem;
import com.example.hubble.data.model.DmConversationItem;
import com.example.hubble.data.model.FriendUserDto;
import com.example.hubble.data.model.MessageDto;
import com.example.hubble.data.model.ServerItem;
import com.example.hubble.data.repository.DmRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.concurrent.atomic.AtomicInteger;

public class MainViewModel extends ViewModel {

    private final DmRepository dmRepository;

    private final MutableLiveData<List<ServerItem>> _servers = new MutableLiveData<>();
    public final LiveData<List<ServerItem>> servers = _servers;

    private final MutableLiveData<ServerItem> _selectedServer = new MutableLiveData<>();
    public final LiveData<ServerItem> selectedServer = _selectedServer;

    private final MutableLiveData<List<ChannelListItem>> _channels = new MutableLiveData<>();
    public final LiveData<List<ChannelListItem>> channels = _channels;

    private final MutableLiveData<List<DmConversationItem>> _dmStories = new MutableLiveData<>();
    public final LiveData<List<DmConversationItem>> dmStories = _dmStories;

    private final MutableLiveData<List<DmConversationItem>> _dmConversations = new MutableLiveData<>();
    public final LiveData<List<DmConversationItem>> dmConversations = _dmConversations;

    private final MutableLiveData<String> _errorMessage = new MutableLiveData<>();
    public final LiveData<String> errorMessage = _errorMessage;

    public MainViewModel(DmRepository dmRepository) {
        this.dmRepository = dmRepository;
        _servers.setValue(new ArrayList<>());
        _selectedServer.setValue(null);
        _channels.setValue(new ArrayList<>());
        refreshDirectMessages();
    }

    public void setServers(List<ServerItem> servers) {
        List<ServerItem> updated = servers != null ? new ArrayList<>(servers) : new ArrayList<>();
        _servers.setValue(updated);

        if (updated.isEmpty()) {
            _selectedServer.setValue(null);
            _channels.setValue(new ArrayList<>());
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
        loadChannelsForServer(server.getId());
    }

    private void loadChannelsForServer(@SuppressWarnings("unused") String serverId) {
        List<ChannelListItem> items = new ArrayList<>();

        ChannelListItem.Category voice = new ChannelListItem.Category("voice", "Voice Channels");
        items.add(voice);
        items.add(new ChannelListItem.Channel("v1", "General",      ChannelListItem.Channel.ChannelType.VOICE, "voice"));
        items.add(new ChannelListItem.Channel("v2", "Lounge",       ChannelListItem.Channel.ChannelType.VOICE, "voice"));

        ChannelListItem.Category text = new ChannelListItem.Category("text", "Text Channels");
        items.add(text);
        items.add(new ChannelListItem.Channel("t1", "general",       ChannelListItem.Channel.ChannelType.TEXT, "text"));
        items.add(new ChannelListItem.Channel("t2", "announcements", ChannelListItem.Channel.ChannelType.TEXT, "text"));
        items.add(new ChannelListItem.Channel("t3", "off-topic",     ChannelListItem.Channel.ChannelType.TEXT, "text"));

        _channels.setValue(items);
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
            return String.format("%02d:%02d", hour, minute);
        } catch (DateTimeParseException ignored) {
            return "";
        }
    }

    private String resolveSenderLabel(String currentUserId, String authorId, String peerDisplayName) {
        if (authorId != null && currentUserId != null && authorId.equalsIgnoreCase(currentUserId)) {
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
