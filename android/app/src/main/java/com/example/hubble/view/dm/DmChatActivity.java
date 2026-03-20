package com.example.hubble.view.dm;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.hubble.BuildConfig;
import com.example.hubble.R;
import com.example.hubble.adapter.dm.DmMessageAdapter;
import com.example.hubble.data.model.dm.DmMessageItem;
import com.example.hubble.data.model.dm.MessageDto;
import com.example.hubble.data.model.auth.UserResponse;
import com.example.hubble.data.repository.DmRepository;
import com.example.hubble.databinding.ActivityDmChatBinding;
import com.example.hubble.utils.TokenManager;
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;

import org.java_websocket.client.WebSocketClient;
import ua.naiksoftware.stomp.Stomp;
import ua.naiksoftware.stomp.StompClient;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class DmChatActivity extends AppCompatActivity {

    private static final String EXTRA_CHANNEL_ID = "extra_channel_id";
    private static final String EXTRA_USERNAME = "extra_username";

    private ActivityDmChatBinding binding;
    private DmMessageAdapter adapter;
    private DmRepository dmRepository;
    private TokenManager tokenManager;
    private StompClient stompClient;
    private CompositeDisposable disposables = new CompositeDisposable();
    private final Gson gson = new Gson();

    private String channelId;
    private String currentUserId;
    private String currentUserName;
    private String peerName;

    public static Intent createIntent(Context context, String channelId, String username) {
        Intent intent = new Intent(context, DmChatActivity.class);
        intent.putExtra(EXTRA_CHANNEL_ID, channelId);
        intent.putExtra(EXTRA_USERNAME, username);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDmChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        dmRepository = new DmRepository(this);
        tokenManager = new TokenManager(this);
        currentUserId = dmRepository.getCurrentUserId();

        UserResponse user = tokenManager.getUser();
        if (user != null) {
            currentUserName = user.getDisplayName() != null ? user.getDisplayName() : user.getUsername();
        }
        if (currentUserName == null) currentUserName = getString(R.string.dm_me);

        channelId = getIntent().getStringExtra(EXTRA_CHANNEL_ID);
        peerName = getIntent().getStringExtra(EXTRA_USERNAME);
        if (TextUtils.isEmpty(peerName)) peerName = getString(R.string.dm_default_user);

        setupToolbar(peerName);
        setupMessageList();
        setupComposer();
        loadMessageHistory();
    }

    @Override
    protected void onStart() {
        super.onStart();
        connectStomp();
    }

    @Override
    protected void onStop() {
        super.onStop();
        disconnectStomp();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        disposables.clear();
    }

    private void setupToolbar(String username) {
        setSupportActionBar(binding.toolbar);
        binding.toolbar.setNavigationOnClickListener(v -> finish());
        binding.tvHeaderName.setText(username);
        binding.tilComposer.setHint(getString(R.string.dm_message_hint, username));
    }

    private void setupMessageList() {
        adapter = new DmMessageAdapter();
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        binding.rvMessages.setLayoutManager(layoutManager);
        binding.rvMessages.setAdapter(adapter);
    }

    private void setupComposer() {
        binding.btnSend.setOnClickListener(v -> {
            String content = binding.etComposer.getText() == null
                    ? "" : binding.etComposer.getText().toString().trim();
            if (content.isEmpty() || TextUtils.isEmpty(channelId)) return;
            binding.etComposer.setText("");
            sendMessage(content);
        });

        binding.btnAttach.setOnClickListener(v ->
                Snackbar.make(binding.getRoot(), getString(R.string.main_coming_soon), Snackbar.LENGTH_SHORT).show());
        binding.btnCall.setOnClickListener(v ->
                Snackbar.make(binding.getRoot(), getString(R.string.main_coming_soon), Snackbar.LENGTH_SHORT).show());
        binding.btnVideo.setOnClickListener(v ->
                Snackbar.make(binding.getRoot(), getString(R.string.main_coming_soon), Snackbar.LENGTH_SHORT).show());
    }

    private void loadMessageHistory() {
        if (TextUtils.isEmpty(channelId)) return;
        dmRepository.getMessages(channelId, 0, 50, result -> {
            if (result.getData() != null) {
                List<MessageDto> ordered = new ArrayList<>(result.getData());
                Collections.reverse(ordered);
                runOnUiThread(() -> {
                    List<DmMessageItem> items = mapMessages(ordered);
                    adapter.setItems(items);
                    if (!items.isEmpty()) binding.rvMessages.scrollToPosition(items.size() - 1);
                });
            }
        });
    }

    private void sendMessage(String content) {
        dmRepository.sendMessage(channelId, content, result -> {
            if (result.getData() != null) {
                runOnUiThread(() -> appendMessage(result.getData()));
            }
        });
    }

    private void connectStomp() {
        if (TextUtils.isEmpty(channelId)) return;

        String wsUrl = BuildConfig.BASE_URL
                .replace("https://", "wss://")
                .replace("http://", "ws://")
                + "ws/websocket";

        String token = tokenManager.getAccessToken();
        stompClient = Stomp.over(Stomp.ConnectionProvider.OKHTTP, wsUrl);

        disposables.add(stompClient.lifecycle()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(event -> {}, throwable -> {}));

        disposables.add(stompClient
                .topic("/topic/channels/" + channelId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(stompMessage -> {
                    MessageDto dto = gson.fromJson(stompMessage.getPayload(), MessageDto.class);
                    if (dto == null) return;
                    // Bỏ qua tin nhắn của chính mình (đã add qua REST callback)
                    boolean isFromMe = currentUserId != null && currentUserId.equals(dto.getAuthorId());
                    if (!isFromMe) appendMessage(dto);
                }, throwable -> {}));

        stompClient.connect();
    }

    private void disconnectStomp() {
        disposables.clear();
        if (stompClient != null) stompClient.disconnect();
    }

    private void appendMessage(MessageDto dto) {
        boolean mine = currentUserId != null && currentUserId.equals(dto.getAuthorId());
        String sender = mine ? getString(R.string.dm_me) : peerName;
        DmMessageItem item = new DmMessageItem(
                dto.getId(), sender,
                dto.getContent() == null ? "" : dto.getContent(),
                formatTime(dto.getCreatedAt()), mine
        );
        adapter.appendItem(item);
        binding.rvMessages.scrollToPosition(adapter.getItemCount() - 1);
    }

    private List<DmMessageItem> mapMessages(List<MessageDto> rawMessages) {
        List<DmMessageItem> mapped = new ArrayList<>();
        for (MessageDto dto : rawMessages) {
            boolean mine = currentUserId != null && currentUserId.equals(dto.getAuthorId());
            String sender = mine ? getString(R.string.dm_me) : peerName;
            mapped.add(new DmMessageItem(
                    dto.getId(), sender,
                    dto.getContent() == null ? "" : dto.getContent(),
                    formatTime(dto.getCreatedAt()), mine
            ));
        }
        return mapped;
    }

    private String formatTime(String rawTime) {
        if (rawTime == null || rawTime.trim().isEmpty()) return "";
        try {
            LocalDateTime dateTime = LocalDateTime.parse(rawTime);
            return dateTime.format(DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault()));
        } catch (Exception ignored) {
            return rawTime;
        }
    }
}
