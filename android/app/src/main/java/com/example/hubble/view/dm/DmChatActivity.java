//package com.example.hubble.view.dm;
//
//import android.content.Context;
//import android.content.Intent;
//import android.os.Bundle;
//import android.text.TextUtils;
//
//import androidx.appcompat.app.AppCompatActivity;
//import androidx.recyclerview.widget.LinearLayoutManager;
//
//import com.example.hubble.R;
//import com.example.hubble.adapter.dm.DmMessageAdapter;
//import com.example.hubble.data.model.dm.MessageDto;
//import com.example.hubble.data.model.dm.DmMessageItem;
//import com.example.hubble.data.model.UserResponse;
//import com.example.hubble.data.realtime.FirestoreMessageRepository;
//import com.example.hubble.data.repository.DmRepository;
//import com.example.hubble.databinding.ActivityDmChatBinding;
//import com.example.hubble.utils.TokenManager;
//import com.google.android.material.snackbar.Snackbar;
//
//import java.time.LocalDateTime;
//import java.time.format.DateTimeFormatter;
//import java.util.ArrayList;
//import java.util.Collections;
//import java.util.List;
//import java.util.Locale;
//
//public class DmChatActivity extends AppCompatActivity {
//
//    private static final String EXTRA_CHANNEL_ID = "extra_channel_id";
//    private static final String EXTRA_USERNAME = "extra_username";
//
//    private ActivityDmChatBinding binding;
//    private DmMessageAdapter adapter;
//    private DmRepository dmRepository;
//    private FirestoreMessageRepository firestoreRepo;
//    private TokenManager tokenManager;
//
//    private FirestoreMessageRepository firestoreRepo;
//    private TokenManager tokenManager;
//
//    private String channelId;
//    private String currentUserId;
//    private String currentUserName;
//    private String currentUserName;
//    private String peerName;
//
//    public static Intent createIntent(Context context, String channelId, String username) {
//        Intent intent = new Intent(context, DmChatActivity.class);
//        intent.putExtra(EXTRA_CHANNEL_ID, channelId);
//        intent.putExtra(EXTRA_USERNAME, username);
//        return intent;
//    }
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        binding = ActivityDmChatBinding.inflate(getLayoutInflater());
//        setContentView(binding.getRoot());
//
//        dmRepository = new DmRepository(this);
//        firestoreRepo = new FirestoreMessageRepository();
//        tokenManager = new TokenManager(this);
//
//        currentUserId = dmRepository.getCurrentUserId();
//
//        UserResponse user = tokenManager.getUser();
//        if (user != null) {
//            currentUserName = user.getDisplayName() != null ? user.getDisplayName() : user.getUsername();
//        }
//        if (currentUserName == null) currentUserName = getString(R.string.dm_me);
//
//        channelId = getIntent().getStringExtra(EXTRA_CHANNEL_ID);
//        peerName = getIntent().getStringExtra(EXTRA_USERNAME);
//        if (TextUtils.isEmpty(peerName)) {
//            peerName = getString(R.string.dm_default_user);
//        }
//
//        setupToolbar(peerName);
//        setupMessageList();
//        setupComposer();
//    }
//
//    @Override
//    protected void onStart() {
//        super.onStart();
//        subscribeRealtime();
//    }
//
//    @Override
//    protected void onStop() {
//        super.onStop();
//        firestoreRepo.removeListener();
//    }
//
//    @Override
//    protected void onDestroy() {
//        super.onDestroy();
//        firestoreRepo.removeListener();
//        firestoreRepo.removeListener();
//    }
//
//    private void setupToolbar(String username) {
//        setSupportActionBar(binding.toolbar);
//        binding.toolbar.setNavigationOnClickListener(v -> finish());
//        binding.tvHeaderName.setText(username);
//        binding.tilComposer.setHint(getString(R.string.dm_message_hint, username));
//    }
//
//    private void setupMessageList() {
//        adapter = new DmMessageAdapter();
//        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
//        layoutManager.setStackFromEnd(true);
//        binding.rvMessages.setLayoutManager(layoutManager);
//        binding.rvMessages.setAdapter(adapter);
//    }
//
//    private void setupComposer() {
//        binding.btnSend.setOnClickListener(v -> {
//            String content = binding.etComposer.getText() == null
//                    ? ""
//                    : binding.etComposer.getText().toString().trim();
//
//            if (content.isEmpty() || TextUtils.isEmpty(channelId)) return;
//
//            binding.etComposer.setText("");
//            firestoreRepo.sendMessage(channelId, currentUserId, currentUserName, content);
//        });
//
//        binding.btnAttach.setOnClickListener(v ->
//                Snackbar.make(binding.getRoot(), getString(R.string.main_coming_soon), Snackbar.LENGTH_SHORT).show());
//
//        binding.btnCall.setOnClickListener(v ->
//                Snackbar.make(binding.getRoot(), getString(R.string.main_coming_soon), Snackbar.LENGTH_SHORT).show());
//
//        binding.btnVideo.setOnClickListener(v ->
//                Snackbar.make(binding.getRoot(), getString(R.string.main_coming_soon), Snackbar.LENGTH_SHORT).show());
//    }
//
//    private void subscribeRealtime() {
//        if (TextUtils.isEmpty(channelId)) return;
//
//        firestoreRepo.listenMessages(channelId, new FirestoreMessageRepository.MessagesListener() {
//            @Override
//            public void onMessages(List<MessageDto> messages) {
//                runOnUiThread(() -> {
//                    List<DmMessageItem> mapped = mapMessages(messages);
//                    adapter.setItems(mapped);
//                    if (!mapped.isEmpty()) {
//                        binding.rvMessages.scrollToPosition(mapped.size() - 1);
//                    }
//                });
//            }
//
//            @Override
//            public void onError(String error) {
//                // Không spam snackbar khi lỗi realtime
//            }
//        });
//    }
//
//    private void subscribeRealtime() {
//        if (TextUtils.isEmpty(channelId)) return;
//
//        firestoreRepo.listenMessages(channelId, new FirestoreMessageRepository.MessagesListener() {
//            @Override
//            public void onMessages(List<MessageDto> messages) {
//                runOnUiThread(() -> {
//                    List<DmMessageItem> mapped = mapMessages(messages);
//                    adapter.setItems(mapped);
//                    if (!mapped.isEmpty()) {
//                        binding.rvMessages.scrollToPosition(mapped.size() - 1);
//                    }
//                });
//            }
//
//            @Override
//            public void onError(String error) {
//                // Không spam snackbar khi lỗi realtime
//            }
//        });
//    }
//
//    private List<DmMessageItem> mapMessages(List<MessageDto> rawMessages) {
//        // Firestore trả về DESC (mới nhất trước), cần đảo lại để hiển thị đúng thứ tự
//        // Firestore trả về DESC (mới nhất trước), cần đảo lại để hiển thị đúng thứ tự
//        List<MessageDto> ordered = new ArrayList<>(rawMessages);
//        Collections.reverse(ordered);
//
//        List<DmMessageItem> mapped = new ArrayList<>();
//        for (MessageDto dto : ordered) {
//            boolean mine = currentUserId != null && currentUserId.equals(dto.getAuthorId());
//            String sender = mine ? getString(R.string.dm_me) : peerName;
//            mapped.add(new DmMessageItem(
//                    dto.getId(),
//                    sender,
//                    dto.getContent() == null ? "" : dto.getContent(),
//                    formatTime(dto.getCreatedAt()),
//                    mine
//            ));
//        }
//        return mapped;
//    }
//
//    private String formatTime(String rawTime) {
//        if (rawTime == null || rawTime.trim().isEmpty()) return "";
//        if (rawTime == null || rawTime.trim().isEmpty()) return "";
//        try {
//            LocalDateTime dateTime = LocalDateTime.parse(rawTime);
//            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault());
//            return dateTime.format(formatter);
//        } catch (Exception ignored) {
//            return rawTime;
//        }
//    }
//
//    private String buildMessageSnapshot(List<MessageDto> messages) {
//        if (messages == null || messages.isEmpty()) {
//            return "empty";
//        }
//
//        MessageDto latest = messages.get(0);
//        String id = latest.getId() == null ? "" : latest.getId();
//        String editedAt = latest.getEditedAt() == null ? "" : latest.getEditedAt();
//        return messages.size() + "|" + id + "|" + editedAt;
//    }
//
//    private void subscribeRealtime() {
//        if (TextUtils.isEmpty(channelId)) {
//            return;
//        }
//
//        dmStompClient.connectAndSubscribe(channelId, new DmStompClient.Listener() {
//            @Override
//            public void onMessage(MessageDto message) {
//                if (message == null) {
//                    return;
//                }
//
//                if (message.getChannelId() != null && !channelId.equals(message.getChannelId())) {
//                    return;
//                }
//
//                runOnUiThread(() -> applyIncomingMessage(message));
//            }
//
//            @Override
//            public void onError(String message) {
//                // Không spam snackbar để tránh ảnh hưởng trải nghiệm chat.
//            }
//        });
//    }
//
//    private void unsubscribeRealtime() {
//        if (dmStompClient != null) {
//            dmStompClient.disconnect();
//        }
//    }
//
//    private void applyIncomingMessage(MessageDto incoming) {
//        String messageId = incoming.getId();
//        if (messageId == null || messageId.trim().isEmpty()) {
//            loadMessages(true);
//            return;
//        }
//
//        boolean changed = false;
//        synchronized (cachedMessagesDesc) {
//            int index = indexOfMessage(cachedMessagesDesc, messageId);
//            if (index >= 0) {
//                cachedMessagesDesc.set(index, incoming);
//                changed = true;
//            } else {
//                // API list đang ở thứ tự createdAt DESC, nên tin mới thêm ở đầu list.
//                cachedMessagesDesc.add(0, incoming);
//                changed = true;
//            }
//        }
//
//        if (!changed) {
//            return;
//        }
//
//        List<MessageDto> rawCopy;
//        synchronized (cachedMessagesDesc) {
//            rawCopy = new ArrayList<>(cachedMessagesDesc);
//        }
//
//        lastMessageSnapshot = buildMessageSnapshot(rawCopy);
//        List<DmMessageItem> mapped = mapMessages(rawCopy);
//        adapter.setItems(mapped);
//        if (!mapped.isEmpty()) {
//            binding.rvMessages.scrollToPosition(mapped.size() - 1);
//        }
//    }
//
//    private int indexOfMessage(List<MessageDto> messages, String messageId) {
//        for (int i = 0; i < messages.size(); i++) {
//            MessageDto dto = messages.get(i);
//            if (dto.getId() != null && dto.getId().equals(messageId)) {
//                return i;
//            }
//        }
//        return -1;
//    }
//}