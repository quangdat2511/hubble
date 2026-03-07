package com.example.hubble.viewmodel;

import android.graphics.Color;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.hubble.data.model.ChannelListItem;
import com.example.hubble.data.model.ServerItem;

import java.util.ArrayList;
import java.util.List;

public class MainViewModel extends ViewModel {

    private final MutableLiveData<List<ServerItem>> _servers = new MutableLiveData<>();
    public final LiveData<List<ServerItem>> servers = _servers;

    private final MutableLiveData<ServerItem> _selectedServer = new MutableLiveData<>();
    public final LiveData<ServerItem> selectedServer = _selectedServer;

    private final MutableLiveData<List<ChannelListItem>> _channels = new MutableLiveData<>();
    public final LiveData<List<ChannelListItem>> channels = _channels;

    public MainViewModel() {
        loadStubServers();
    }

    private void loadStubServers() {
        List<ServerItem> list = new ArrayList<>();
        list.add(new ServerItem("1", "Hubble",        null, Color.parseColor("#5865F2")));
        list.add(new ServerItem("2", "Máy cũ",        null, Color.parseColor("#57F287")));
        list.add(new ServerItem("3", "DSA",           null, Color.parseColor("#FEE75C")));
        list.add(new ServerItem("4", "Gaming",        null, Color.parseColor("#ED4245")));
        list.add(new ServerItem("5", "Team Project",  null, Color.parseColor("#EB459E")));
        _servers.setValue(list);
        selectServer(list.get(0));
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
}
