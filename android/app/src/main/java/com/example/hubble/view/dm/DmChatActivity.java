package com.example.hubble.view.dm;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.activity.OnBackPressedCallback;

import com.bumptech.glide.Glide;
import com.example.hubble.BuildConfig;
import com.example.hubble.R;
import com.example.hubble.adapter.dm.DmMessageAdapter;
import com.example.hubble.data.model.auth.AuthResult;
import com.example.hubble.data.model.dm.DmMessageItem;
import com.example.hubble.data.model.dm.MessageDto;
import com.example.hubble.data.model.auth.UserResponse;
import com.example.hubble.data.repository.DmRepository;
import com.example.hubble.databinding.ActivityDmChatBinding;
import com.example.hubble.utils.TokenManager;
import com.example.hubble.viewmodel.MediaViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;

import ua.naiksoftware.stomp.Stomp;
import ua.naiksoftware.stomp.StompClient;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

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

    // Constants cho bàn phím
    private static final float KEYBOARD_HEIGHT_RATIO = 0.15f;
    private static final int DEFAULT_PANEL_DP = 300;

    private ActivityDmChatBinding binding;
    private DmMessageAdapter adapter;
    private DmRepository dmRepository;
    private TokenManager tokenManager;
    private MediaViewModel mediaViewModel;

    // STOMP (Từ nhánh main)
    private StompClient stompClient;
    private CompositeDisposable disposables = new CompositeDisposable();
    private final Gson gson = new Gson();

    // Dữ liệu người dùng & UI
    private String channelId;
    private String currentUserId;
    private String currentUserName;
    private String peerName;
    private String lastMessageSnapshot = "";
    private final List<MessageDto> cachedMessagesDesc = new ArrayList<>();

    // File / Media (Từ nhánh media)
    private final List<String> pendingAttachmentIds = new ArrayList<>();
    private final List<String> pendingAttachmentTypes = new ArrayList<>();
    private MediaRecorder mediaRecorder;
    private File audioFile;
    private boolean isRecording = false;
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;

    // Đếm thời gian ghi âm
    private int recordSeconds = 0;
    private Handler recordHandler = new Handler(Looper.getMainLooper());
    private Runnable recordRunnable;

    // Bàn phím / Emoji (Từ nhánh main)
    private boolean isEmojiPanelVisible = false;
    private int keyboardHeight = 0;
    private boolean pendingShowKeyboard = false;

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
        mediaViewModel = new ViewModelProvider(this).get(MediaViewModel.class);
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
        setupEmojiPanel();
        setupKeyboardHeightDetection();
        loadMessages(false);
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (isEmojiPanelVisible) {
                    hideEmojiPanel(false); // Nếu đang mở bảng Emoji thì đóng lại
                } else {
                    setEnabled(false); // Tắt cờ bắt sự kiện này
                    getOnBackPressedDispatcher().onBackPressed(); // Gọi lệnh Back mặc định (thoát ra)
                }
            }
        });
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

    // ─────────────────────────────────────────────────────────────────────────
    // Setup UI cơ bản
    // ─────────────────────────────────────────────────────────────────────────

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

        binding.rvMessages.setOnTouchListener((v, event) -> {
            if (isEmojiPanelVisible) hideEmojiPanel(false);
            return false;
        });
    }

    private void setupComposer() {
        binding.btnSend.setOnClickListener(v -> attemptSendMessage());

        binding.etComposer.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN) {
                if (event.isShiftPressed()) return false;
                attemptSendMessage();
                return true;
            }
            return false;
        });

        // Click Emoji -> Mở Keyboard
        binding.etComposer.setOnClickListener(v -> {
            if (isEmojiPanelVisible) hideEmojiPanel(true);
        });

        binding.etComposer.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus && isEmojiPanelVisible) hideEmojiPanel(true);
        });

        // Nút Media
        binding.btnAttach.setOnClickListener(v -> filePickerLauncher.launch("*/*"));
        binding.btnCall.setOnClickListener(v -> Snackbar.make(binding.getRoot(), getString(R.string.main_coming_soon), Snackbar.LENGTH_SHORT).show());
        binding.btnVideo.setOnClickListener(v -> Snackbar.make(binding.getRoot(), getString(R.string.main_coming_soon), Snackbar.LENGTH_SHORT).show());

        binding.btnVoice.setOnClickListener(v -> {
            if (!isRecording) {
                startRecording();
                binding.btnVoice.setIconResource(android.R.drawable.ic_media_pause);
                binding.btnSend.setEnabled(false);
                binding.btnAttach.setEnabled(false);
            } else {
                stopRecording();
                binding.btnVoice.setIconResource(android.R.drawable.ic_btn_speak_now);
                binding.btnSend.setEnabled(true);
                binding.btnAttach.setEnabled(true);
            }
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // File Picker & Upload (Nhánh Media)
    // ─────────────────────────────────────────────────────────────────────────

    private final ActivityResultLauncher<String> filePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetMultipleContents(), uris -> {
                if (uris != null && !uris.isEmpty()) handleFilesSelected(uris);
            });

    private void handleFilesSelected(List<Uri> uris) {
        binding.btnAttach.setEnabled(false);
        binding.llAttachmentPreviews.removeAllViews();
        pendingAttachmentIds.clear();
        pendingAttachmentTypes.clear();

        int[] completed = {0};
        int total = uris.size();

        for (Uri uri : uris) {
            mediaViewModel.uploadMedia(uri).observe(this, result -> {
                switch (result.status) {
                    case LOADING: break;
                    case SUCCESS:
                        pendingAttachmentIds.add(result.data.getAttachmentId());
                        pendingAttachmentTypes.add(result.data.getContentType());
                        showAttachmentPreview(uri, result.data.getAttachmentId(), result.data.getContentType(), result.data.getFilename());                        completed[0]++;
                        if (completed[0] == total) {
                            binding.btnAttach.setEnabled(true);
                            Snackbar.make(binding.getRoot(), total + " file(s) ready", Snackbar.LENGTH_SHORT).show();
                        }
                        break;
                    case ERROR:
                        completed[0]++;
                        Snackbar.make(binding.getRoot(), result.errorMessage, Snackbar.LENGTH_SHORT).show();
                        if (completed[0] == total) binding.btnAttach.setEnabled(true);
                        break;
                }
            });
        }
    }

    private void showAttachmentPreview(Uri uri, String attachmentId, String contentType, String filename) {
        binding.attachmentPreviewBar.setVisibility(View.VISIBLE);

        View previewView = getLayoutInflater().inflate(R.layout.item_attachment_preview, binding.llAttachmentPreviews, false);
        ImageView ivPreview = previewView.findViewById(R.id.ivPreview);
        ImageView ivFileIcon = previewView.findViewById(R.id.ivFileIcon);
        View btnRemove = previewView.findViewById(R.id.btnRemove); // Đổi kiểu thành View chung

        if (contentType != null && (contentType.startsWith("image/") || contentType.startsWith("video/"))) {
            ivPreview.setVisibility(View.VISIBLE);
            ivFileIcon.setVisibility(View.GONE);
            Glide.with(this).load(uri).centerCrop().into(ivPreview);
        } else {
            ivPreview.setVisibility(View.GONE);
            ivFileIcon.setVisibility(View.VISIBLE);

            String lowerMime = contentType != null ? contentType.toLowerCase() : "";
            String lowerName = filename != null ? filename.toLowerCase() : "";

            if (lowerMime.contains("pdf") || lowerName.endsWith(".pdf")) {
                ivFileIcon.setImageResource(R.drawable.ic_file_pdf);
            }
            else if (lowerMime.contains("word") || lowerMime.contains("document") || lowerName.endsWith(".docx") || lowerName.endsWith(".doc")) {
                ivFileIcon.setImageResource(R.drawable.ic_file_docx);
            }
            else if (lowerMime.contains("excel") || lowerMime.contains("spreadsheet") || lowerName.endsWith(".xlsx") || lowerName.endsWith(".xls")) {
                ivFileIcon.setImageResource(R.drawable.ic_file_excel);
            }
            else if (lowerMime.contains("powerpoint") || lowerMime.contains("presentation") || lowerName.endsWith(".pptx") || lowerName.endsWith(".ppt")) {
                ivFileIcon.setImageResource(R.drawable.ic_file_powerpoint);
            }
            else if (lowerMime.contains("zip") || lowerMime.contains("rar") || lowerName.endsWith(".zip") || lowerName.endsWith(".rar")) {
                ivFileIcon.setImageResource(R.drawable.ic_file_zip);
            }
            else if (lowerMime.startsWith("text/") || lowerName.endsWith(".txt")) {
                ivFileIcon.setImageResource(R.drawable.ic_file_text);
            }
            else {
                ivFileIcon.setImageResource(R.drawable.ic_file_generic);
            }
        }

        btnRemove.setOnClickListener(v -> {
            binding.llAttachmentPreviews.removeView(previewView); // Chỉ xóa đúng cái ảnh/file này

            int index = pendingAttachmentIds.indexOf(attachmentId);
            if (index != -1) {
                pendingAttachmentIds.remove(index);
                pendingAttachmentTypes.remove(index);
            }

            // Nếu đã bấm X xóa hết sạch ảnh/file rồi thì ẩn luôn thanh cuộn đi
            if (pendingAttachmentIds.isEmpty()) {
                clearAttachmentPreview();
            }
        });

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

    // ─────────────────────────────────────────────────────────────────────────
    // Ghi âm (Nhánh Media)
    // ─────────────────────────────────────────────────────────────────────────

    private void startRecording() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_RECORD_AUDIO_PERMISSION);
            return;
        }

        audioFile = new File(getExternalCacheDir(), "voice_" + System.currentTimeMillis() + ".m4a");
        mediaRecorder = new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        mediaRecorder.setOutputFile(audioFile.getAbsolutePath());

        try {
            mediaRecorder.prepare();
            mediaRecorder.start();
            isRecording = true;
            recordSeconds = 0;
            recordRunnable = new Runnable() {
                @Override
                public void run() {
                    binding.tilComposer.setHint(String.format("Đang ghi âm... %d:%02d", recordSeconds / 60, recordSeconds % 60));
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
                if (audioFile != null && audioFile.exists()) audioFile.delete();
            } finally {
                mediaRecorder.release();
                mediaRecorder = null;
                isRecording = false;
            }

            recordHandler.removeCallbacks(recordRunnable);
            binding.tilComposer.setHint(getString(R.string.dm_message_hint, peerName));

            if (audioFile != null && audioFile.exists() && audioFile.length() > 0 && recordSeconds >= 1) {
                uploadVoiceAndSend(audioFile);
            } else {
                if (audioFile != null && audioFile.exists()) audioFile.delete();
                Snackbar.make(binding.getRoot(), "Ghi âm quá ngắn!", Snackbar.LENGTH_SHORT).show();
            }
        }
    }

    private void uploadVoiceAndSend(File file) {
        Uri fileUri = Uri.fromFile(file);
        mediaViewModel.uploadMedia(fileUri).observe(this, result -> {
            if (result.status == com.example.hubble.data.repository.MediaRepository.UploadResult.Status.SUCCESS) {
                List<String> attachIds = new ArrayList<>();
                attachIds.add(result.data.getAttachmentId());
                dmRepository.sendMessage(channelId, "", attachIds, "VOICE", sendResult -> {
                    if (sendResult.getStatus() == AuthResult.Status.SUCCESS) loadMessages(false);
                });
            }
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Emoji & Bàn phím (Nhánh Main)
    // ─────────────────────────────────────────────────────────────────────────

    private void setupEmojiPanel() {
        binding.btnEmoji.setOnClickListener(v -> toggleEmojiPanel());
        binding.emojiPickerView.setOnEmojiSelectedListener(this::insertEmojiAtCursor);
        binding.emojiPickerView.setOnMediaSelectedListener(new com.example.hubble.view.emoji.EmojiPickerView.OnMediaSelectedListener() {
            @Override
            public void onGifSelected(String gifUrl, String previewUrl, String title) {
                sendMediaMessage(DmMessageAdapter.GIF_PREFIX + encodeMediaContent(title, gifUrl));
            }
            @Override
            public void onStickerSelected(String stickerUrl, String previewUrl, String title) {
                sendMediaMessage(DmMessageAdapter.STICKER_PREFIX + encodeMediaContent(title, stickerUrl));
            }
        });
    }

    private void sendMediaMessage(String content) {
        if (TextUtils.isEmpty(channelId)) return;
        hideEmojiPanel(false);
        // Gửi Giphy/Sticker như một TEXT kèm danh sách đính kèm trống
        dmRepository.sendMessage(channelId, content, new ArrayList<>(), "TEXT", result -> {
            if (result.getStatus() == AuthResult.Status.SUCCESS) loadMessages(false);
        });
    }

    private static String encodeMediaContent(String title, String url) {
        if (title != null && !title.trim().isEmpty()) return title.trim() + "\n" + url;
        return url;
    }

    private void setupKeyboardHeightDetection() {
        binding.getRoot().getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            Rect visibleFrame = new Rect();
            binding.getRoot().getWindowVisibleDisplayFrame(visibleFrame);
            int screenHeight = binding.getRoot().getRootView().getHeight();
            int detectedKeyboardHeight = screenHeight - visibleFrame.bottom;

            if (detectedKeyboardHeight > screenHeight * KEYBOARD_HEIGHT_RATIO) {
                if (detectedKeyboardHeight != keyboardHeight) {
                    keyboardHeight = detectedKeyboardHeight;
                    if (isEmojiPanelVisible) setPanelHeight(keyboardHeight);
                }
                if (isEmojiPanelVisible && !pendingShowKeyboard) collapseEmojiPanel();
                pendingShowKeyboard = false;
            }
        });
    }

    private void toggleEmojiPanel() {
        if (isEmojiPanelVisible) hideEmojiPanel(true);
        else showEmojiPanel();
    }

    private void showEmojiPanel() {
        isEmojiPanelVisible = true;
        binding.btnEmoji.setIconResource(R.drawable.ic_keyboard_open);
        int height = resolveEmojiPanelHeight();
        hideKeyboard();
        setPanelHeight(height);
        binding.emojiPickerContainer.setVisibility(View.VISIBLE);
        binding.emojiPickerContainer.setTranslationY(height);
        binding.emojiPickerContainer.animate().translationY(0).setDuration(220).setInterpolator(new DecelerateInterpolator(1.5f)).start();
    }

    private void hideEmojiPanel(boolean showKeyboard) {
        if (!isEmojiPanelVisible) return;
        collapseEmojiPanel();
        if (showKeyboard) {
            pendingShowKeyboard = true;
            binding.etComposer.requestFocus();
            showKeyboard();
        }
    }

    private void collapseEmojiPanel() {
        isEmojiPanelVisible = false;
        binding.btnEmoji.setIconResource(R.drawable.ic_emoji);
        int height = resolveEmojiPanelHeight();
        binding.emojiPickerContainer.animate().translationY(height).setDuration(180).setInterpolator(new DecelerateInterpolator(1.5f))
                .withEndAction(() -> {
                    binding.emojiPickerContainer.setVisibility(View.GONE);
                    binding.emojiPickerContainer.setTranslationY(0);
                    setPanelHeight(0);
                }).start();
    }

    private void setPanelHeight(int height) {
        ViewGroup.LayoutParams params = binding.emojiPickerContainer.getLayoutParams();
        params.height = height;
        binding.emojiPickerContainer.setLayoutParams(params);
    }

    private int resolveEmojiPanelHeight() {
        return keyboardHeight > 0 ? keyboardHeight : (int) (DEFAULT_PANEL_DP * getResources().getDisplayMetrics().density);
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        View focusedView = getCurrentFocus();
        if (focusedView != null && imm != null) imm.hideSoftInputFromWindow(focusedView.getWindowToken(), 0);
    }

    private void showKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.showSoftInput(binding.etComposer, InputMethodManager.SHOW_IMPLICIT);
    }

    private void insertEmojiAtCursor(String emoji) {
        Editable editable = binding.etComposer.getText();
        if (editable == null) {
            binding.etComposer.setText(emoji);
            return;
        }
        int start = Math.max(binding.etComposer.getSelectionStart(), 0);
        int end = Math.max(binding.etComposer.getSelectionEnd(), 0);
        if (start > end) { int tmp = start; start = end; end = tmp; }
        editable.replace(start, end, emoji);
        binding.etComposer.setSelection(start + emoji.length());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Gửi và Tải Tin nhắn (Kết hợp Main + Media)
    // ─────────────────────────────────────────────────────────────────────────

    private void attemptSendMessage() {
        String content = binding.etComposer.getText() == null ? "" : binding.etComposer.getText().toString().trim();
        if (content.isEmpty() && pendingAttachmentIds.isEmpty()) return;

        String messageType = "TEXT";
        if (!pendingAttachmentTypes.isEmpty()) {
            boolean hasFile = false;
            for (String type : pendingAttachmentTypes) {
                if (type != null && !type.startsWith("image/") && !type.startsWith("video/")) {
                    hasFile = true; break;
                }
            }
            messageType = hasFile ? "FILE" : "IMAGE";
        }

        dmRepository.sendMessage(channelId, content, pendingAttachmentIds, messageType, result -> {
            if (result.getStatus() == AuthResult.Status.SUCCESS) {
                binding.etComposer.post(() -> {
                    binding.etComposer.setText("");
                    clearAttachmentPreview();
                });
                loadMessages(false);
            } else {
                String error = result.getMessage() != null ? result.getMessage() : getString(R.string.error_generic);
                Snackbar.make(binding.getRoot(), error, Snackbar.LENGTH_SHORT).show();
            }
        });
    }

    private void loadMessages(boolean silent) {
        if (TextUtils.isEmpty(channelId)) return;
        dmRepository.getMessages(channelId, 0, 50, result -> {
            if (result.getStatus() == AuthResult.Status.SUCCESS && result.getData() != null) {
                String snapshot = buildMessageSnapshot(result.getData());
                if (snapshot.equals(lastMessageSnapshot)) return;

                List<DmMessageItem> mapped = mapMessages(result.getData());
                synchronized (cachedMessagesDesc) {
                    cachedMessagesDesc.clear();
                    cachedMessagesDesc.addAll(result.getData());
                }
                adapter.setItems(mapped);
                lastMessageSnapshot = snapshot;
                if (!mapped.isEmpty()) binding.rvMessages.scrollToPosition(mapped.size() - 1);
            } else if (!silent) {
                String error = result.getMessage() != null ? result.getMessage() : getString(R.string.error_generic);
                Snackbar.make(binding.getRoot(), error, Snackbar.LENGTH_SHORT).show();
            }
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // STOMP Connection (Từ nhánh Main) + Render list (Từ nhánh Media)
    // ─────────────────────────────────────────────────────────────────────────

    private void connectStomp() {
        if (TextUtils.isEmpty(channelId)) return;
        String wsUrl = BuildConfig.BASE_URL.replace("https://", "wss://").replace("http://", "ws://") + "ws";
        stompClient = Stomp.over(Stomp.ConnectionProvider.OKHTTP, wsUrl);

        disposables.add(stompClient.lifecycle()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(event -> {
                    if (event.getType() == ua.naiksoftware.stomp.dto.LifecycleEvent.Type.OPENED) subscribeToChannel();
                }, throwable -> {}));
        stompClient.connect();
    }

    private void subscribeToChannel() {
        if (TextUtils.isEmpty(channelId) || stompClient == null) return;
        disposables.add(stompClient
                .topic("/topic/channels/" + channelId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(stompMessage -> {
                    MessageDto dto = gson.fromJson(stompMessage.getPayload(), MessageDto.class);
                    if (dto == null) return;
                    runOnUiThread(() -> applyIncomingMessage(dto));
                }, throwable -> {}));
    }

    private void disconnectStomp() {
        disposables.clear();
        if (stompClient != null) stompClient.disconnect();
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
                cachedMessagesDesc.add(0, incoming);
                changed = true;
            }
        }

        if (!changed) return;

        List<MessageDto> rawCopy;
        synchronized (cachedMessagesDesc) { rawCopy = new ArrayList<>(cachedMessagesDesc); }

        lastMessageSnapshot = buildMessageSnapshot(rawCopy);
        List<DmMessageItem> mapped = mapMessages(rawCopy);
        adapter.setItems(mapped);
        if (!mapped.isEmpty()) binding.rvMessages.scrollToPosition(mapped.size() - 1);
    }

    private int indexOfMessage(List<MessageDto> messages, String messageId) {
        for (int i = 0; i < messages.size(); i++) {
            if (messages.get(i).getId() != null && messages.get(i).getId().equals(messageId)) return i;
        }
        return -1;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private List<DmMessageItem> mapMessages(List<MessageDto> rawMessages) {
        List<MessageDto> ordered = new ArrayList<>(rawMessages);
        Collections.reverse(ordered);
        List<DmMessageItem> mapped = new ArrayList<>();
        for (MessageDto dto : ordered) {
            boolean mine = currentUserId != null && currentUserId.equals(dto.getAuthorId());
            String sender = mine ? getString(R.string.dm_me) : peerName;
            mapped.add(new DmMessageItem(
                    dto.getId(), sender,
                    dto.getContent() == null ? "" : dto.getContent(),
                    formatTime(dto.getCreatedAt()), mine, dto.getAttachments()
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

    private String buildMessageSnapshot(List<MessageDto> messages) {
        if (messages == null || messages.isEmpty()) return "empty";
        MessageDto latest = messages.get(0);
        return messages.size() + "|" + (latest.getId() == null ? "" : latest.getId()) + "|" + (latest.getEditedAt() == null ? "" : latest.getEditedAt());
    }
}