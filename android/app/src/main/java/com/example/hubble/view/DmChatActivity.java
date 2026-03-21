package com.example.hubble.view;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
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

import java.io.File;
import java.io.IOException;
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
    private final List<String> pendingAttachmentTypes = new ArrayList<>();

    private MediaRecorder mediaRecorder;
    private File audioFile;
    private boolean isRecording = false;
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;

    // Biến cho bộ đếm thời gian
    private int recordSeconds = 0;
    private Handler recordHandler = new Handler(Looper.getMainLooper());
    private Runnable recordRunnable;

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
        if (contentType != null && (contentType.startsWith("image/") || contentType.startsWith("video/"))) {
            ivPreview.setVisibility(View.VISIBLE);
            ivFileIcon.setVisibility(View.GONE);
            Glide.with(this).load(uri).centerCrop().into(ivPreview);
        } else {
            ivPreview.setVisibility(View.GONE);
            ivFileIcon.setVisibility(View.VISIBLE);
            // Lưu ý: Nếu trong layout item_attachment_preview.xml có
            // TextView để hiện tên file, hãy setText(filename) cho nó ở đây.
        }

        btnRemove.setOnClickListener(v -> clearAttachmentPreview());

        binding.llAttachmentPreviews.addView(previewView);
    }

    private void clearAttachmentPreview() {
        pendingAttachmentIds.clear();
        pendingAttachmentTypes.clear();
        binding.attachmentPreviewBar.setVisibility(View.GONE);
        binding.llAttachmentPreviews.removeAllViews();
        binding.tilComposer.setHint(getString(R.string.dm_message_hint, peerName));
        binding.btnAttach.setEnabled(true);
    }

    private void handleFilesSelected(List<Uri> uris) {
        binding.btnAttach.setEnabled(false);
        binding.llAttachmentPreviews.removeAllViews();
        pendingAttachmentIds.clear();
        pendingAttachmentTypes.clear();

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
                        pendingAttachmentTypes.add(result.data.getContentType());
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

            String messageType = "TEXT";
            if (!pendingAttachmentTypes.isEmpty()) {
                boolean hasFile = false;
                for (String type : pendingAttachmentTypes) {
                    if (type != null && !type.startsWith("image/") && !type.startsWith("video/")) {
                        hasFile = true;
                        break;
                    }
                }
                // Nếu có ít nhất 1 tệp tài liệu -> đánh dấu là FILE.
                // Nếu chỉ toàn ảnh/video -> đánh dấu là IMAGE
                messageType = hasFile ? "FILE" : "IMAGE";
            }

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
                filePickerLauncher.launch("*/*"));

        binding.btnVoice.setOnClickListener(v -> {
            if (!isRecording) {
                startRecording();
                binding.btnVoice.setIconResource(android.R.drawable.ic_media_pause); // Đổi icon sang nút Pause/Stop
                binding.btnSend.setEnabled(false); // Khóa nút Send text
                binding.btnAttach.setEnabled(false); // Khóa nút đính kèm
            } else {
                stopRecording();
                binding.btnVoice.setIconResource(android.R.drawable.ic_btn_speak_now); // Trả lại icon Mic
                binding.btnSend.setEnabled(true);
                binding.btnAttach.setEnabled(true);
            }
        });

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

    private void startRecording() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_RECORD_AUDIO_PERMISSION);
            return;
        }

        String fileName = "voice_" + System.currentTimeMillis() + ".m4a";
        audioFile = new File(getExternalCacheDir(), fileName);

        mediaRecorder = new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        mediaRecorder.setOutputFile(audioFile.getAbsolutePath());

        try {
            mediaRecorder.prepare();
            mediaRecorder.start();
            isRecording = true;

            // Bắt đầu đếm thời gian
            recordSeconds = 0;
            recordRunnable = new Runnable() {
                @Override
                public void run() {
                    int minutes = recordSeconds / 60;
                    int seconds = recordSeconds % 60;
                    binding.tilComposer.setHint(String.format("Đang ghi âm... %d:%02d", minutes, seconds));
                    recordSeconds++;
                    recordHandler.postDelayed(this, 1000);
                }
            };
            recordHandler.post(recordRunnable);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void stopRecording() {
        if (mediaRecorder != null && isRecording) {
            try {
                mediaRecorder.stop();
            } catch (RuntimeException stopException) {
                // Chặn crash nếu người dùng bấm stop quá nhanh (nhấp nhả liên tục)
                if (audioFile != null && audioFile.exists()) {
                    audioFile.delete();
                }
            } finally {
                mediaRecorder.release();
                mediaRecorder = null;
                isRecording = false;
            }

            // Dừng đếm thời gian và UI
            recordHandler.removeCallbacks(recordRunnable);
            binding.tilComposer.setHint(getString(R.string.dm_message_hint, peerName));

            if (audioFile != null && audioFile.exists()) {
                long fileSize = audioFile.length();
                android.util.Log.d("VOICE_TEST", "Dung lượng file ghi âm: " + fileSize + " bytes");

                if (fileSize == 0) {
                    android.util.Log.e("VOICE_TEST", "Toang! File rỗng 0 byte, có thể do bấm stop quá nhanh hoặc mất quyền mic.");
                } else {
                    android.util.Log.d("VOICE_TEST", "Thu âm ngon lành! Lỗi chắc chắn ở phần gọi API Upload.");
                }
            }

            // CHỈ UPLOAD NẾU FILE TỒN TẠI, CÓ DUNG LƯỢNG (>0 BYTE) VÀ DÀI HƠN 1 GIÂY
            if (audioFile != null && audioFile.exists() && audioFile.length() > 0 && recordSeconds >= 1) {
                uploadVoiceAndSend(audioFile);
            } else {
                // Xóa file rác và báo lỗi
                if (audioFile != null && audioFile.exists()) {
                    audioFile.delete();
                }
                // Import com.google.android.material.snackbar.Snackbar; nếu cần
                Snackbar.make(binding.getRoot(), "Ghi âm quá ngắn!", Snackbar.LENGTH_SHORT).show();
            }
        }
    }

    private void uploadVoiceAndSend(File file) {
        Uri fileUri = Uri.fromFile(file);

        mediaViewModel.uploadMedia(fileUri).observe(this, result -> {
            switch (result.status) {
                case LOADING:
                    // Tùy chọn: Hiện một cái ProgressDialog hoặc Toast báo "Đang gửi ghi âm..."
                    break;

                case SUCCESS:
                    // 1. Lấy ID file vừa upload xong
                    List<String> attachIds = new ArrayList<>();
                    attachIds.add(result.data.getAttachmentId());

                    // 2. Bắn tin nhắn qua WebSocket/API
                    dmRepository.sendMessage(channelId, "", attachIds, "VOICE", sendResult -> {
                        if (sendResult.getStatus() == AuthResult.Status.SUCCESS) {
                            loadMessages(false); // Cập nhật lại list chat
                        }
                    });
                    break;

                case ERROR:
                    // Hiện thông báo nếu upload file ghi âm bị lỗi
                    Snackbar.make(binding.getRoot(), "Lỗi gửi ghi âm: " + result.errorMessage, Snackbar.LENGTH_SHORT).show();
                    break;
            }
        });
    }
}
