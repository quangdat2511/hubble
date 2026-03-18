package com.example.hubble.view.dm;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.hubble.R;
import com.example.hubble.adapter.dm.DmMessageAdapter;
import com.example.hubble.data.model.AuthResult;
import com.example.hubble.data.model.dm.MessageDto;
import com.example.hubble.data.model.dm.DmMessageItem;
import com.example.hubble.data.repository.DmRepository;
import com.example.hubble.data.realtime.DmStompClient;
import com.example.hubble.databinding.ActivityDmChatBinding;
import com.example.hubble.utils.TokenManager;
import com.google.android.material.snackbar.Snackbar;

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
    private DmStompClient dmStompClient;
    private String channelId;
    private String currentUserId;
    private String peerName;
    private String lastMessageSnapshot = "";
    private final List<MessageDto> cachedMessagesDesc = new ArrayList<>();

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
        dmStompClient = new DmStompClient(new TokenManager(this));
        currentUserId = dmRepository.getCurrentUserId();

        channelId = getIntent().getStringExtra(EXTRA_CHANNEL_ID);

        peerName = getIntent().getStringExtra(EXTRA_USERNAME);
        if (TextUtils.isEmpty(peerName)) {
            peerName = getString(R.string.dm_default_user);
        }

        setupToolbar(peerName);
        setupMessageList();
        setupComposer();
        loadMessages(false);
    }

    @Override
    protected void onStart() {
        super.onStart();
        subscribeRealtime();
    }

    @Override
    protected void onStop() {
        super.onStop();
        unsubscribeRealtime();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unsubscribeRealtime();
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

    private void loadMessages(boolean silent) {
        if (TextUtils.isEmpty(channelId)) {
            if (!silent) {
                Snackbar.make(binding.getRoot(), getString(R.string.error_generic), Snackbar.LENGTH_SHORT).show();
            }
            return;
        }

        dmRepository.getMessages(channelId, 0, 50, result -> {
            if (result.getStatus() != AuthResult.Status.SUCCESS || result.getData() == null) {
                if (!silent) {
                    String error = result.getMessage() != null ? result.getMessage() : getString(R.string.error_generic);
                    Snackbar.make(binding.getRoot(), error, Snackbar.LENGTH_SHORT).show();
                }
                return;
            }

            String snapshot = buildMessageSnapshot(result.getData());
            if (snapshot.equals(lastMessageSnapshot)) {
                return;
            }

            List<DmMessageItem> mapped = mapMessages(result.getData());
            synchronized (cachedMessagesDesc) {
                cachedMessagesDesc.clear();
                cachedMessagesDesc.addAll(result.getData());
            }
            adapter.setItems(mapped);
            lastMessageSnapshot = snapshot;
            if (!mapped.isEmpty()) {
                binding.rvMessages.scrollToPosition(mapped.size() - 1);
            }
        });
    }

    private void setupComposer() {
        binding.btnSend.setOnClickListener(v -> {
            String content = binding.etComposer.getText() == null
                    ? ""
                    : binding.etComposer.getText().toString().trim();

            if (content.isEmpty()) {
                return;
            }

            dmRepository.sendMessage(channelId, content, result -> {
                if (result.getStatus() == AuthResult.Status.SUCCESS) {
                    binding.etComposer.post(() -> binding.etComposer.setText(""));
                    loadMessages(false);
                    return;
                }

                String error = result.getMessage() != null ? result.getMessage() : getString(R.string.error_generic);
                Snackbar.make(binding.getRoot(), error, Snackbar.LENGTH_SHORT).show();
            });
        });

        binding.btnAttach.setOnClickListener(v ->
                Snackbar.make(binding.getRoot(), getString(R.string.main_coming_soon), Snackbar.LENGTH_SHORT).show());

        binding.btnCall.setOnClickListener(v ->
                Snackbar.make(binding.getRoot(), getString(R.string.main_coming_soon), Snackbar.LENGTH_SHORT).show());

        binding.btnVideo.setOnClickListener(v ->
                Snackbar.make(binding.getRoot(), getString(R.string.main_coming_soon), Snackbar.LENGTH_SHORT).show());
    }

    private List<DmMessageItem> mapMessages(List<MessageDto> rawMessages) {
        List<MessageDto> ordered = new ArrayList<>(rawMessages);
        Collections.reverse(ordered);

        List<DmMessageItem> mapped = new ArrayList<>();
        for (MessageDto dto : ordered) {
            boolean mine = currentUserId != null && currentUserId.equals(dto.getAuthorId());
            String sender = mine ? getString(R.string.dm_me) : peerName;
            mapped.add(new DmMessageItem(
                    dto.getId(),
                    sender,
                    dto.getContent() == null ? "" : dto.getContent(),
                    formatTime(dto.getCreatedAt()),
                    mine
            ));
        }
        return mapped;
    }

    private String formatTime(String rawTime) {
        if (rawTime == null || rawTime.trim().isEmpty()) {
            return "";
        }

        try {
            LocalDateTime dateTime = LocalDateTime.parse(rawTime);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault());
            return dateTime.format(formatter);
        } catch (Exception ignored) {
            return rawTime;
        }
    }

    private String buildMessageSnapshot(List<MessageDto> messages) {
        if (messages == null || messages.isEmpty()) {
            return "empty";
        }

        MessageDto latest = messages.get(0);
        String id = latest.getId() == null ? "" : latest.getId();
        String editedAt = latest.getEditedAt() == null ? "" : latest.getEditedAt();
        return messages.size() + "|" + id + "|" + editedAt;
    }

    private void subscribeRealtime() {
        if (TextUtils.isEmpty(channelId)) {
            return;
        }

        dmStompClient.connectAndSubscribe(channelId, new DmStompClient.Listener() {
            @Override
            public void onMessage(MessageDto message) {
                if (message == null) {
                    return;
                }

                if (message.getChannelId() != null && !channelId.equals(message.getChannelId())) {
                    return;
                }

                runOnUiThread(() -> applyIncomingMessage(message));
            }

            @Override
            public void onError(String message) {
                // Không spam snackbar để tránh ảnh hưởng trải nghiệm chat.
            }
        });
    }

    private void unsubscribeRealtime() {
        if (dmStompClient != null) {
            dmStompClient.disconnect();
        }
    }

    private void applyIncomingMessage(MessageDto incoming) {
        String messageId = incoming.getId();
        if (messageId == null || messageId.trim().isEmpty()) {
            loadMessages(true);
            return;
        }

        boolean changed = false;
        synchronized (cachedMessagesDesc) {
            int index = indexOfMessage(cachedMessagesDesc, messageId);
            if (index >= 0) {
                cachedMessagesDesc.set(index, incoming);
                changed = true;
            } else {
                // API list đang ở thứ tự createdAt DESC, nên tin mới thêm ở đầu list.
                cachedMessagesDesc.add(0, incoming);
                changed = true;
            }
        }

        if (!changed) {
            return;
        }

        List<MessageDto> rawCopy;
        synchronized (cachedMessagesDesc) {
            rawCopy = new ArrayList<>(cachedMessagesDesc);
        }

        lastMessageSnapshot = buildMessageSnapshot(rawCopy);
        List<DmMessageItem> mapped = mapMessages(rawCopy);
        adapter.setItems(mapped);
        if (!mapped.isEmpty()) {
            binding.rvMessages.scrollToPosition(mapped.size() - 1);
        }
    }

    private int indexOfMessage(List<MessageDto> messages, String messageId) {
        for (int i = 0; i < messages.size(); i++) {
            MessageDto dto = messages.get(i);
            if (dto.getId() != null && dto.getId().equals(messageId)) {
                return i;
            }
        }
        return -1;
    }
}


