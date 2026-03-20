package com.example.hubble.view;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.example.hubble.R;
import com.example.hubble.adapter.DmMessageAdapter;
import com.example.hubble.data.model.AuthResult;
import com.example.hubble.data.model.DmMessageItem;
import com.example.hubble.data.model.MessageDto;
import com.example.hubble.data.repository.DmRepository;
import com.example.hubble.data.realtime.DmStompClient;
import com.example.hubble.databinding.ActivityDmChatBinding;
import com.example.hubble.utils.TokenManager;
import com.example.hubble.viewmodel.MediaViewModel;
import com.google.android.material.button.MaterialButton;
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

    private MediaViewModel mediaViewModel;
    private final List<String> pendingAttachmentIds = new ArrayList<>();

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
        mediaViewModel = new ViewModelProvider(this).get(MediaViewModel.class);
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

    private final ActivityResultLauncher<String> filePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetMultipleContents(), uris -> {
                if (uris != null && !uris.isEmpty()) {
                    handleFilesSelected(uris);
                }
            });

    private void showAttachmentPreview(Uri uri, String filename, String contentType) {
        binding.attachmentPreviewBar.setVisibility(View.VISIBLE);
        binding.llAttachmentPreviews.removeAllViews();

        View previewView = getLayoutInflater()
                .inflate(R.layout.item_attachment_preview, binding.llAttachmentPreviews, false);

        ImageView ivPreview = previewView.findViewById(R.id.ivPreview);
        ImageView ivFileIcon = previewView.findViewById(R.id.ivFileIcon);
        MaterialButton btnRemove = previewView.findViewById(R.id.btnRemove);

        // Show image thumbnail or file icon depending on type
        if (contentType != null && contentType.startsWith("image/")) {
            ivPreview.setVisibility(View.VISIBLE);
            ivFileIcon.setVisibility(View.GONE);
            Glide.with(this).load(uri).centerCrop().into(ivPreview);
        } else {
            ivPreview.setVisibility(View.GONE);
            ivFileIcon.setVisibility(View.VISIBLE);
        }

        btnRemove.setOnClickListener(v -> clearAttachmentPreview());

        binding.llAttachmentPreviews.addView(previewView);
    }

    private void clearAttachmentPreview() {
        pendingAttachmentIds.clear();   // ← was pendingAttachmentId = null
        binding.attachmentPreviewBar.setVisibility(View.GONE);
        binding.llAttachmentPreviews.removeAllViews();
        binding.tilComposer.setHint(getString(R.string.dm_message_hint, peerName));
        binding.btnAttach.setEnabled(true);
    }

    private void handleFilesSelected(List<Uri> uris) {
        binding.btnAttach.setEnabled(false);
        binding.llAttachmentPreviews.removeAllViews();
        pendingAttachmentIds.clear();

        // Track how many uploads complete
        int[] completed = {0};
        int total = uris.size();

        for (Uri uri : uris) {
            mediaViewModel.uploadMedia(uri).observe(this, result -> {
                switch (result.status) {
                    case LOADING:
                        break;

                    case SUCCESS:
                        pendingAttachmentIds.add(result.data.getAttachmentId());
                        showAttachmentPreview(uri, result.data.getFilename(), result.data.getContentType());
                        completed[0]++;
                        if (completed[0] == total) {
                            binding.btnAttach.setEnabled(true);
                            Snackbar.make(binding.getRoot(),
                                    total + " file(s) ready — tap send",
                                    Snackbar.LENGTH_SHORT).show();
                        }
                        break;

                    case ERROR:
                        completed[0]++;
                        Snackbar.make(binding.getRoot(), result.errorMessage, Snackbar.LENGTH_SHORT).show();
                        if (completed[0] == total) {
                            binding.btnAttach.setEnabled(true);
                        }
                        break;
                }
            });
        }
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
                    ? "" : binding.etComposer.getText().toString().trim();

            if (content.isEmpty() && pendingAttachmentIds.isEmpty()) return;

            //  pass the full list
            String messageType = pendingAttachmentIds.isEmpty() ? "TEXT" : "IMAGE";

            dmRepository.sendMessage(channelId, content, pendingAttachmentIds, messageType, result -> {
                if (result.getStatus() == AuthResult.Status.SUCCESS) {
                    binding.etComposer.post(() -> {
                        binding.etComposer.setText("");
                        clearAttachmentPreview();
                    });
                    loadMessages(false);
                    return;
                }
                String error = result.getMessage() != null
                        ? result.getMessage() : getString(R.string.error_generic);
                Snackbar.make(binding.getRoot(), error, Snackbar.LENGTH_SHORT).show();
            });
        });;

        // launches file picker
        binding.btnAttach.setOnClickListener(v ->
                filePickerLauncher.launch("image/*"));

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
                    mine,
                    dto.getAttachments()
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
