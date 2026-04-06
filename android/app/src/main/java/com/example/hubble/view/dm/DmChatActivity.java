package com.example.hubble.view.dm;

import android.Manifest;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.animation.ObjectAnimator;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.example.hubble.R;
import com.example.hubble.adapter.dm.DmMessageAdapter;
import com.example.hubble.adapter.dm.ForwardTargetAdapter;
import com.example.hubble.adapter.dm.ReplySwipeCallback;
import com.example.hubble.data.api.NetworkConfig;
import com.example.hubble.data.model.auth.AuthResult;
import com.example.hubble.data.model.auth.UserResponse;
import com.example.hubble.data.model.dm.ChannelDto;
import com.example.hubble.data.model.dm.DmMessageItem;
import com.example.hubble.data.model.dm.MessageDto;
import com.example.hubble.data.model.dm.ReactionDto;
import com.example.hubble.data.repository.DmRepository;
import com.example.hubble.databinding.ActivityDmChatBinding;
import com.example.hubble.databinding.BottomSheetForwardMessageBinding;
import com.example.hubble.databinding.BottomSheetMessageActionsBinding;
import com.example.hubble.databinding.DialogDeleteMessageBinding;
import com.example.hubble.utils.AvatarPlaceholderUtils;
import com.example.hubble.utils.TokenManager;
import com.example.hubble.viewmodel.MediaViewModel;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import ua.naiksoftware.stomp.Stomp;
import ua.naiksoftware.stomp.StompClient;
import ua.naiksoftware.stomp.dto.StompHeader;

import okhttp3.Dns;
import okhttp3.OkHttpClient;

public class DmChatActivity extends AppCompatActivity {

    private static final String EXTRA_CHANNEL_ID = "extra_channel_id";
    private static final String EXTRA_USERNAME = "extra_username";
    private static final String EXTRA_AVATAR_URL = "extra_avatar_url";
    private static final String RAILWAY_HOST = "hubble-production.up.railway.app";
    private static final String[] RAILWAY_FALLBACK_IPS = {
            "151.101.2.15"
    };

    private static final float KEYBOARD_HEIGHT_RATIO = 0.15f;
    private static final int DEFAULT_PANEL_DP = 300;
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;

    private ActivityDmChatBinding binding;
    private DmMessageAdapter adapter;
    private DmRepository dmRepository;
    private TokenManager tokenManager;
    private MediaViewModel mediaViewModel;

    private StompClient stompClient;
    private CompositeDisposable disposables = new CompositeDisposable();
    private final Gson gson = new Gson();

    private String channelId;
    private String currentUserId;
    private String currentUserName;
    private String currentUserAvatarUrl;
    private String peerUserId;
    private String peerDisplayName;
    private String peerUsername;
    private String peerAvatarUrl;

    // Emoji/keyboard state (Main)
    private boolean isEmojiPanelVisible = false;
    private int keyboardHeight = 0;
    private boolean pendingShowKeyboard = false;
    private DmMessageItem replyingToItem;
    private DmMessageItem editingItem;

    // Tracks server-assigned IDs for messages that are currently in-flight (optimistic).
    // When STOMP echoes back our own message, we skip it — the HTTP response handles it.
    private final Set<String> pendingServerIds = new HashSet<>();

    private String lastMarkedReadMessageId;
    private final Runnable flushMarkReadRunnable = this::flushMarkChannelRead;

    // File / Media state (Của bạn)
    private final List<String> pendingAttachmentIds = new ArrayList<>();
    private final List<String> pendingAttachmentTypes = new ArrayList<>();
    private MediaRecorder mediaRecorder;
    private File audioFile;
    private boolean isRecording = false;
    private int recordTicks = 0;
    private android.media.MediaPlayer previewPlayer ;

    private Handler recordHandler = new Handler(Looper.getMainLooper());
    private Runnable recordRunnable;

    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    private final Runnable hideCopyBannerRunnable = this::hideCopyBanner;
    private boolean isKeyboardVisible = false;

    // Typing indicator
    private static final long TYPING_SEND_INTERVAL_MS = 2000L;
    private static final long TYPING_HIDE_DELAY_MS = 3500L;
    private long lastTypingSentMillis = 0L;
    private final ObjectAnimator[] dotAnimators = new ObjectAnimator[3];
    private final Runnable hideTypingRunnable = this::hideTypingIndicator;

    public static Intent createIntent(Context context, String channelId, String username) {
        return createIntent(context, channelId, username, null);
    }

    public static Intent createIntent(Context context, String channelId, String username, String avatarUrl) {
        Intent intent = new Intent(context, DmChatActivity.class);
        intent.putExtra(EXTRA_CHANNEL_ID, channelId);
        intent.putExtra(EXTRA_USERNAME, username);
        intent.putExtra(EXTRA_AVATAR_URL, avatarUrl);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        super.onCreate(savedInstanceState);
        binding = ActivityDmChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());


        dmRepository = new DmRepository(this);
        tokenManager = new TokenManager(this);
        mediaViewModel = new ViewModelProvider(this).get(MediaViewModel.class);
        currentUserId = dmRepository.getCurrentUserId();

        UserResponse user = tokenManager.getUser();
        if (user != null) {
            currentUserName = firstNonBlank(user.getDisplayName(), user.getUsername());
            currentUserAvatarUrl = toAbsoluteAvatarUrl(user.getAvatarUrl());
        }
        if (currentUserName == null) currentUserName = getString(R.string.dm_me);

        channelId = getIntent().getStringExtra(EXTRA_CHANNEL_ID);
        peerDisplayName = getIntent().getStringExtra(EXTRA_USERNAME);
        if (TextUtils.isEmpty(peerDisplayName)) {
            peerDisplayName = getString(R.string.dm_default_user);
        }
        peerUsername = null;
        peerAvatarUrl = toAbsoluteAvatarUrl(getIntent().getStringExtra(EXTRA_AVATAR_URL));

        setupToolbar();
        setupProfileIntro();
        setupMessageList();
        setupComposer();
        setupEmojiPanel();
        setupKeyboardHeightDetection();
        if (shouldLoadPeerProfile()) {
            loadPeerProfile();
        }
        loadMessageHistory();

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (isEmojiPanelVisible) {
                    hideEmojiPanel(false);
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
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
    protected void onResume() {
        super.onResume();
        scheduleMarkChannelRead();
    }

    @Override
    protected void onStop() {
        super.onStop();
        disconnectStomp();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        disconnectStomp();
        disposables.clear();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Setup UI & Peer info (Main)
    // ─────────────────────────────────────────────────────────────────────────

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        binding.toolbar.setNavigationOnClickListener(v -> finish());
        binding.headerInfo.setOnClickListener(v -> openConversationDetails());
        binding.ivHeaderChevron.setOnClickListener(v -> openConversationDetails());
        refreshPeerUi();
    }

    private void setupProfileIntro() {
        binding.tvProfileIntroDisplayName.setText(peerDisplayName);
        binding.tvProfileIntroUsername.setText(formatUsername(peerUsername));
        binding.tvProfileIntroDesc.setText(getString(R.string.dm_profile_intro_desc, peerDisplayName));

        bindAvatar(binding.ivProfileIntroAvatar, peerAvatarUrl, peerDisplayName);
    }

    private void loadPeerProfile() {
        if (TextUtils.isEmpty(channelId)) return;
        dmRepository.getDirectChannels(result -> {
            if (result.getStatus() != AuthResult.Status.SUCCESS || result.getData() == null) return;
            for (ChannelDto channel : result.getData()) {
                if (channel != null && channelId.equals(channel.getId())) {
                    runOnUiThread(() -> applyPeerProfile(channel));
                    return;
                }
            }
        });
    }

    private boolean shouldLoadPeerProfile() {
        return TextUtils.isEmpty(peerAvatarUrl) || TextUtils.isEmpty(peerUsername);
    }

    private void applyPeerProfile(ChannelDto channel) {
        peerUserId = firstNonBlank(channel.getPeerUserId(), peerUserId);
        peerDisplayName = firstNonBlank(channel.getPeerDisplayName(), channel.getPeerUsername(), peerDisplayName, getString(R.string.dm_default_user));
        peerUsername = firstNonBlank(channel.getPeerUsername(), peerUsername, peerDisplayName);
        peerAvatarUrl = toAbsoluteAvatarUrl(firstNonBlank(channel.getPeerAvatarUrl(), peerAvatarUrl));
        refreshPeerUi();
    }

    private void openConversationDetails() {
        if (TextUtils.isEmpty(channelId)) {
            Snackbar.make(binding.getRoot(), R.string.dm_gallery_error_generic, Snackbar.LENGTH_SHORT).show();
            return;
        }
        startActivity(DmDetailsActivity.createIntent(
                this,
                channelId,
                peerDisplayName,
                peerUsername,
                peerAvatarUrl
        ));
    }

    private void refreshPeerUi() {
        if (TextUtils.isEmpty(peerDisplayName)) {
            peerDisplayName = getString(R.string.dm_default_user);
        }
        String mentionTarget = firstNonBlank(stripLeadingAt(peerUsername), peerDisplayName);

        binding.tvHeaderName.setText(peerDisplayName);
        binding.etComposer.setHint(getString(R.string.dm_message_hint, mentionTarget));
        binding.tvProfileIntroDisplayName.setText(peerDisplayName);
        binding.tvProfileIntroUsername.setText(formatUsername(peerUsername));
        binding.tvProfileIntroDesc.setText(getString(R.string.dm_profile_intro_desc, peerDisplayName));
        bindAvatar(binding.ivHeaderAvatar, peerAvatarUrl, peerDisplayName);
        bindAvatar(binding.ivProfileIntroAvatar, peerAvatarUrl, peerDisplayName);
        if (adapter != null) {
            adapter.setParticipantAvatarUrls(currentUserAvatarUrl, peerAvatarUrl);
            adapter.setIntroItem(DmMessageItem.createIntro(
                    peerDisplayName,
                    formatUsername(peerUsername),
                    getString(R.string.dm_profile_intro_desc, peerDisplayName)));
        }
    }

    private String formatUsername(String username) {
        String safeUsername = firstNonBlank(
                stripLeadingAt(username),
                stripLeadingAt(peerUsername),
                peerDisplayName,
                getString(R.string.dm_default_user)
        );
        return getString(R.string.dm_username_format, safeUsername);
    }

    private String stripLeadingAt(String value) {
        if (TextUtils.isEmpty(value)) {
            return value;
        }
        String normalized = value.trim();
        while (normalized.startsWith("@")) {
            normalized = normalized.substring(1).trim();
        }
        return normalized;
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (!TextUtils.isEmpty(value) && !value.trim().isEmpty()) return value;
        }
        return null;
    }

    private String toAbsoluteAvatarUrl(String avatarUrl) {
        return NetworkConfig.resolveUrl(avatarUrl);
    }

    private void bindAvatar(ImageView imageView, String avatarUrl, String displayName) {
        int avatarSize = imageView.getLayoutParams() != null ? imageView.getLayoutParams().width : imageView.getWidth();
        android.graphics.drawable.Drawable avatarFallback =
                AvatarPlaceholderUtils.createAvatarDrawable(this, displayName, avatarSize);
        String resolvedAvatarUrl = toAbsoluteAvatarUrl(avatarUrl);

        Glide.with(this).clear(imageView);
        if (TextUtils.isEmpty(resolvedAvatarUrl)) {
            imageView.setImageDrawable(avatarFallback);
            return;
        }

        imageView.setImageDrawable(null);
        Glide.with(this)
                .load(resolvedAvatarUrl)
                .error(avatarFallback)
                .fallback(avatarFallback)
                .circleCrop()
                .into(imageView);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Danh sách tin nhắn (Merge: Swipe to Reply, List, Touch)
    // ─────────────────────────────────────────────────────────────────────────

    private void setupMessageList() {
        adapter = new DmMessageAdapter();
        adapter.setCurrentUserId(currentUserId);
        adapter.setParticipantAvatarUrls(currentUserAvatarUrl, peerAvatarUrl);
        adapter.setIntroItem(DmMessageItem.createIntro(
                peerDisplayName,
                formatUsername(peerUsername),
                getString(R.string.dm_profile_intro_desc, peerDisplayName)));
        adapter.setOnMessageLongClickListener((item, anchorView) -> showMessageActionsSheet(item));
        adapter.setOnReactionClickListener((item, emoji) -> toggleReaction(item.getId(), emoji));
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        binding.rvMessages.setLayoutManager(layoutManager);
        binding.rvMessages.setAdapter(adapter);

        ReplySwipeCallback swipeCallback = new ReplySwipeCallback(this, position -> {
            DmMessageItem item = adapter.getItem(position);
            if (item != null && !item.isDateSeparator() && !item.isIntro()) startReply(item);
        });
        new ItemTouchHelper(swipeCallback).attachToRecyclerView(binding.rvMessages);

        binding.rvMessages.setOnTouchListener((v, event) -> {
            if (isEmojiPanelVisible) hideEmojiPanel(false);
            return false;
        });

        // No scroll-based intro visibility — intro is inside the RecyclerView as first item.
    }


    // ─────────────────────────────────────────────────────────────────────────
    // Composer & Media (Merge)
    // ─────────────────────────────────────────────────────────────────────────

    private void setupComposer() {
        binding.btnSend.setOnClickListener(v -> attemptSendMessage());
        binding.btnCancelReply.setOnClickListener(v -> clearReply());
        binding.btnCancelEdit.setOnClickListener(v -> clearEditMode(true));

        binding.etComposer.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN) {
                if (event.isShiftPressed()) return false;
                attemptSendMessage();
                return true;
            }
            return false;
        });

        binding.etComposer.setOnClickListener(v -> { if (isEmojiPanelVisible) hideEmojiPanel(true); });
        binding.etComposer.setOnFocusChangeListener((v, hasFocus) -> { if (hasFocus && isEmojiPanelVisible) hideEmojiPanel(true); });

        binding.etComposer.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Nếu người dùng bắt đầu gõ, tự động ẩn gợi ý đi
                if (s.length() > 0 && binding.suggestionBar.getVisibility() == View.VISIBLE) {
                    binding.suggestionBar.setVisibility(View.GONE);
                }
            }
            @Override
            public void afterTextChanged(Editable s) {
                boolean hasText = s != null && s.length() > 0;
                updateComposerState();
                if (hasText) {
                    long now = System.currentTimeMillis();
                    if (now - lastTypingSentMillis >= TYPING_SEND_INTERVAL_MS) {
                        lastTypingSentMillis = now;
                        sendTypingEvent();
                    }
                }
            }
        });

        binding.btnCollapse.setOnClickListener(v -> {
            binding.etComposer.getText().clear();
            updateComposerState();
        });

        binding.btnAttach.setOnClickListener(v -> filePickerLauncher.launch("*/*"));
        binding.btnCall.setOnClickListener(v -> Snackbar.make(binding.getRoot(), getString(R.string.main_coming_soon), Snackbar.LENGTH_SHORT).show());
        binding.btnVideo.setOnClickListener(v -> Snackbar.make(binding.getRoot(), getString(R.string.main_coming_soon), Snackbar.LENGTH_SHORT).show());

//        binding.btnVoice.setOnClickListener(v -> {
//            if (!isRecording) {
//                startRecording();
//                binding.btnVoice.setIconResource(android.R.drawable.ic_media_pause);
//                binding.btnSend.setEnabled(false);
//                binding.btnAttach.setEnabled(false);
//            } else {
//                stopRecording();
//                binding.btnVoice.setIconResource(android.R.drawable.ic_btn_speak_now);
//                binding.btnSend.setEnabled(true);
//                binding.btnAttach.setEnabled(true);
//            }
//        });
        // Khi bấm nút Mic, mở BottomSheet
        binding.btnVoice.setOnClickListener(v -> showVoiceRecordSheet());
    }

    private void showVoiceRecordSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View sheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_voice_record, null);
        dialog.setContentView(sheetView);

        TextView tvStatusHint = sheetView.findViewById(R.id.tvStatusHint);
        View llWaveform = sheetView.findViewById(R.id.llWaveform);
        TextView tvTimer = sheetView.findViewById(R.id.tvTimer);
        View btnDelete = sheetView.findViewById(R.id.btnDelete);
        View btnListen = sheetView.findViewById(R.id.btnListen);
        ImageView ivListenIcon = sheetView.findViewById(R.id.ivListenIcon);
        com.google.android.material.floatingactionbutton.FloatingActionButton btnMainAction = sheetView.findViewById(R.id.btnMainAction);
        // Khai báo ánh xạ View sóng âm
        com.example.hubble.view.custom.AudioWaveformView audioWaveform = sheetView.findViewById(R.id.audioWaveform);

        // State: 0 = IDLE (Chờ), 1 = RECORDING (Đang ghi), 2 = PREVIEW (Xem trước)
        final int[] state = {0};

        Runnable updateUI = () -> {
            if (state[0] == 0) {
                tvStatusHint.setVisibility(View.VISIBLE);
                llWaveform.setVisibility(View.INVISIBLE);
                btnDelete.setVisibility(View.INVISIBLE);
                btnListen.setVisibility(View.INVISIBLE);
                btnMainAction.setImageResource(android.R.drawable.ic_btn_speak_now);
                tvTimer.setText("00:00");
            } else if (state[0] == 1) {
                tvStatusHint.setVisibility(View.INVISIBLE);
                llWaveform.setVisibility(View.VISIBLE);
                btnDelete.setVisibility(View.INVISIBLE);
                btnListen.setVisibility(View.INVISIBLE);
                btnMainAction.setImageResource(android.R.drawable.ic_media_pause); // Icon Dừng
            } else if (state[0] == 2) {
                tvStatusHint.setVisibility(View.INVISIBLE);
                llWaveform.setVisibility(View.VISIBLE);
                btnDelete.setVisibility(View.VISIBLE);
                btnListen.setVisibility(View.VISIBLE);
                btnMainAction.setImageResource(android.R.drawable.ic_menu_send); // Icon Gửi
                ivListenIcon.setImageResource(android.R.drawable.ic_media_play);
            }
        };

        btnMainAction.setOnClickListener(v -> {
            if (state[0] == 0) {
                // KIỂM TRA QUYỀN VÀ BẮT ĐẦU GHI ÂM
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_RECORD_AUDIO_PERMISSION);
                    dialog.dismiss();
                    return;
                }
                internalStartRecord(tvTimer, audioWaveform);
                state[0] = 1;
                updateUI.run();

            } else if (state[0] == 1) {
                // DỪNG GHI ÂM VÀ CHUYỂN SANG PREVIEW
                internalStopRecord();
                if (recordTicks < 10) {
                    Snackbar.make(binding.getRoot(), "Ghi âm quá ngắn!", Snackbar.LENGTH_SHORT).show();
                    state[0] = 0;
                } else {
                    state[0] = 2;
                }
                updateUI.run();

            } else if (state[0] == 2) {
                // BẤM GỬI FILE GHI ÂM
                if (audioFile != null && audioFile.exists()) {
                    uploadVoiceAndSend(audioFile);
                }
                state[0] = 0; // Reset state để dismiss không xóa mất file
                dialog.dismiss();
            }
        });

        btnDelete.setOnClickListener(v -> {
            if (previewPlayer != null) { previewPlayer.release(); previewPlayer = null; }
            if (audioFile != null && audioFile.exists()) audioFile.delete();
            state[0] = 0;
            updateUI.run();
        });

        btnListen.setOnClickListener(v -> {
            if (previewPlayer == null) {
                previewPlayer = new android.media.MediaPlayer();
                try {
                    previewPlayer.setDataSource(audioFile.getAbsolutePath());
                    previewPlayer.prepare();
                    previewPlayer.setOnCompletionListener(mp -> ivListenIcon.setImageResource(android.R.drawable.ic_media_play));
                } catch (IOException e) { e.printStackTrace(); }
            }
            if (previewPlayer.isPlaying()) {
                previewPlayer.pause();
                ivListenIcon.setImageResource(android.R.drawable.ic_media_play);
            } else {
                previewPlayer.start();
                ivListenIcon.setImageResource(android.R.drawable.ic_media_pause);
            }
        });

        dialog.setOnDismissListener(d -> {
            if (state[0] == 1) internalStopRecord(); // Nếu đang thu mà tắt ngang
            if (previewPlayer != null) { previewPlayer.release(); previewPlayer = null; }
            if (state[0] != 0 && audioFile != null && audioFile.exists()) audioFile.delete(); // Xóa file nháp
            recordHandler.removeCallbacks(recordRunnable);
        });

        updateUI.run();
        dialog.show();
    }

    private void internalStartRecord(android.widget.TextView tvTimer, com.example.hubble.view.custom.AudioWaveformView audioWaveform) {
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
            recordTicks = 0; // Reset bộ đếm tick

            recordRunnable = new Runnable() {
                @Override
                public void run() {
                    recordTicks++; // Mỗi 100ms tăng lên 1
                    int currentSeconds = recordTicks / 10; // Chia 10 để ra số giây chuẩn

                    // Cập nhật text đồng hồ
                    tvTimer.setText(String.format("%02d:%02d", currentSeconds / 60, currentSeconds % 60));

                    // Lấy âm lượng và truyền cho View vẽ sóng
                    if (mediaRecorder != null) {
                        int amplitude = mediaRecorder.getMaxAmplitude();
                        audioWaveform.addAmplitude((float) amplitude);
                    }

                    // Chạy lại hàm này sau mỗi 100ms
                    recordHandler.postDelayed(this, 100);
                }
            };
            recordHandler.post(recordRunnable);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void internalStopRecord() {
        if (mediaRecorder != null && isRecording) {
            try { mediaRecorder.stop(); } catch (RuntimeException e) {}
            finally {
                mediaRecorder.release();
                mediaRecorder = null;
                isRecording = false;
            }
            recordHandler.removeCallbacks(recordRunnable);
        }
    }

    private void updateComposerState() {
        boolean hasText = binding.etComposer.getText() != null && binding.etComposer.getText().length() > 0;
        boolean hasAttachment = !pendingAttachmentIds.isEmpty();

        // Điều kiện để gửi là: Có chữ HOẶC có file đính kèm
        boolean canSend = hasText || hasAttachment;

        // Nút Gửi và nút Mic sẽ đổi chỗ cho nhau dựa trên canSend
        binding.btnVoice.setVisibility(canSend ? View.GONE : View.VISIBLE);
        binding.btnSend.setVisibility(canSend ? View.VISIBLE : View.GONE);

        // Nút Đính kèm (+) và nút Mũi tên (>) thì chỉ phụ thuộc vào việc có gõ chữ hay không
        // (Để khi user mới chỉ thêm ảnh, họ vẫn thấy nút (+) để bấm thêm ảnh nữa)
        binding.btnAttach.setVisibility(hasText ? View.GONE : View.VISIBLE);
        binding.btnCollapse.setVisibility(hasText ? View.VISIBLE : View.GONE);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // File Picker & Upload (Của bạn)
    // ─────────────────────────────────────────────────────────────────────────

    private final ActivityResultLauncher<String> filePickerLauncher = registerForActivityResult(new ActivityResultContracts.GetMultipleContents(), uris -> {
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
                        showAttachmentPreview(uri, result.data.getAttachmentId(), result.data.getContentType(), result.data.getFilename());
                        completed[0]++;
                        if (completed[0] == total) {
                            binding.btnAttach.setEnabled(true);
                            Snackbar.make(binding.getRoot(), total + " file(s) ready", Snackbar.LENGTH_SHORT).show();
                            updateComposerState();
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
        View btnRemove = previewView.findViewById(R.id.btnRemove);

        if (contentType != null && (contentType.startsWith("image/") || contentType.startsWith("video/"))) {
            ivPreview.setVisibility(View.VISIBLE);
            ivFileIcon.setVisibility(View.GONE);
            Glide.with(this).load(uri).centerCrop().into(ivPreview);
        } else {
            ivPreview.setVisibility(View.GONE);
            ivFileIcon.setVisibility(View.VISIBLE);

            String lowerMime = contentType != null ? contentType.toLowerCase() : "";
            String lowerName = filename != null ? filename.toLowerCase() : "";

            if (lowerMime.contains("pdf") || lowerName.endsWith(".pdf")) ivFileIcon.setImageResource(R.drawable.ic_file_pdf);
            else if (lowerMime.contains("word") || lowerMime.contains("document") || lowerName.endsWith(".docx") || lowerName.endsWith(".doc")) ivFileIcon.setImageResource(R.drawable.ic_file_docx);
            else if (lowerMime.contains("excel") || lowerMime.contains("spreadsheet") || lowerName.endsWith(".xlsx") || lowerName.endsWith(".xls")) ivFileIcon.setImageResource(R.drawable.ic_file_excel);
            else if (lowerMime.contains("powerpoint") || lowerMime.contains("presentation") || lowerName.endsWith(".pptx") || lowerName.endsWith(".ppt")) ivFileIcon.setImageResource(R.drawable.ic_file_powerpoint);
            else if (lowerMime.contains("zip") || lowerMime.contains("rar") || lowerName.endsWith(".zip") || lowerName.endsWith(".rar")) ivFileIcon.setImageResource(R.drawable.ic_file_zip);
            else if (lowerMime.startsWith("text/") || lowerName.endsWith(".txt")) ivFileIcon.setImageResource(R.drawable.ic_file_text);
            else ivFileIcon.setImageResource(R.drawable.ic_file_generic);
        }

        btnRemove.setOnClickListener(v -> {
            binding.llAttachmentPreviews.removeView(previewView);
            int index = pendingAttachmentIds.indexOf(attachmentId);
            if (index != -1) {
                pendingAttachmentIds.remove(index);
                pendingAttachmentTypes.remove(index);
            }
            updateComposerState();
            if (pendingAttachmentIds.isEmpty()) clearAttachmentPreview();
        });

        binding.llAttachmentPreviews.addView(previewView);
    }

    private void clearAttachmentPreview() {
        pendingAttachmentIds.clear();
        pendingAttachmentTypes.clear();
        binding.attachmentPreviewBar.setVisibility(View.GONE);
        binding.llAttachmentPreviews.removeAllViews();
        binding.etComposer.setHint(getString(R.string.dm_message_hint, peerDisplayName));
        binding.btnAttach.setEnabled(true);
        updateComposerState();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Ghi âm (Của bạn)
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
            recordTicks = 0;
            recordRunnable = new Runnable() {
                @Override
                public void run() {
                    binding.tilComposer.setHint(String.format("Đang ghi âm... %d:%02d", recordTicks / 60, recordTicks % 60));
                    recordTicks++;
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
            binding.etComposer.setHint(getString(R.string.dm_message_hint, peerDisplayName));

            if (audioFile != null && audioFile.exists() && audioFile.length() > 0 && recordTicks >= 1) {
                uploadVoiceAndSend(audioFile);
            } else {
                if (audioFile != null && audioFile.exists()) audioFile.delete();
                Snackbar.make(binding.getRoot(), "Ghi âm quá ngắn!", Snackbar.LENGTH_SHORT).show();
            }
        }
    }

    private void uploadVoiceAndSend(File file) {
        Uri fileUri = Uri.fromFile(file);
        // Optimistic UI for voice
        String tempId = "tmp_" + System.currentTimeMillis();
        DmMessageItem optimisticItem = buildOptimisticItem(tempId, "🎤 Đang gửi tin nhắn thoại…", "VOICE");
        adapter.appendItem(optimisticItem);
        scrollToBottom();

        mediaViewModel.uploadMedia(fileUri).observe(this, result -> {
            if (result.status == com.example.hubble.data.repository.MediaRepository.UploadResult.Status.SUCCESS) {
                List<String> attachIds = new ArrayList<>();
                attachIds.add(result.data.getAttachmentId());
                dmRepository.sendMessage(channelId, null, "", attachIds, "VOICE", sendResult -> {
                    if (sendResult.getStatus() == AuthResult.Status.SUCCESS && sendResult.getData() != null) {
                        String serverId = sendResult.getData().getId();
                        if (serverId != null) pendingServerIds.add(serverId);
                        runOnUiThread(() -> {
                            DmMessageItem realItem = mapMessage(sendResult.getData());
                            realItem.setStatus(resolvePostSendStatus(tempId));
                            adapter.replaceTempItem(tempId, realItem);
                            if (serverId != null) pendingServerIds.remove(serverId);
                            scrollToBottom();
                        });
                    } else {
                        runOnUiThread(() -> adapter.removeItemById(tempId));
                    }
                });
            } else if (result.status == com.example.hubble.data.repository.MediaRepository.UploadResult.Status.ERROR) {
                runOnUiThread(() -> adapter.removeItemById(tempId));
            }
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Emoji & Keyboard (Main)
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
        clearEditMode(false);
        String replyId = replyingToItem != null ? replyingToItem.getId() : null;

        // Optimistic UI
        String tempId = "tmp_" + System.currentTimeMillis();
        DmMessageItem optimisticItem = buildOptimisticItem(tempId, content, "TEXT");
        adapter.appendItem(optimisticItem);
        scrollToBottom();
        clearReply();

        dmRepository.sendMessage(channelId, replyId, content, new ArrayList<>(), "TEXT", result -> {
            if (result.getData() != null) {
                String serverId = result.getData().getId();
                if (serverId != null) pendingServerIds.add(serverId);
                runOnUiThread(() -> {
                    DmMessageItem realItem = mapMessage(result.getData());
                    realItem.setStatus(resolvePostSendStatus(tempId));
                    adapter.replaceTempItem(tempId, realItem);
                    if (serverId != null) pendingServerIds.remove(serverId);
                    scrollToBottom();
                });
            } else {
                runOnUiThread(() -> adapter.removeItemById(tempId));
            }
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
                        boolean keyboardNowVisible = detectedKeyboardHeight > screenHeight * KEYBOARD_HEIGHT_RATIO;

            if (keyboardNowVisible) {
                if (detectedKeyboardHeight != keyboardHeight) {
                    keyboardHeight = detectedKeyboardHeight;
                    if (isEmojiPanelVisible) setPanelHeight(keyboardHeight);
                }
                if (isEmojiPanelVisible && !pendingShowKeyboard) collapseEmojiPanel();
                if (!isKeyboardVisible) {
                    isKeyboardVisible = true;
                }
                pendingShowKeyboard = false;
            } else if (isKeyboardVisible) {
                isKeyboardVisible = false;
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
    // Messaging (Merge Main logic with Media Payload)
    // ─────────────────────────────────────────────────────────────────────────

    private void attemptSendMessage() {
        String content = binding.etComposer.getText() == null ? "" : binding.etComposer.getText().toString();
        String trimmed = content.trim();

        if (trimmed.isEmpty() && pendingAttachmentIds.isEmpty()) return;
        if (TextUtils.isEmpty(channelId)) return;

        if (editingItem != null) {
            commitEdit(trimmed);
            return;
        }

        binding.etComposer.setText("");
        String replyId = replyingToItem != null ? replyingToItem.getId() : null;

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

        // Optimistic UI: show message immediately with SENDING status
        String tempId = "tmp_" + System.currentTimeMillis();
        DmMessageItem optimisticItem = buildOptimisticItem(tempId, trimmed, messageType);
        adapter.appendItem(optimisticItem);
        scrollToBottom();

        List<String> attachmentIdsCopy = new ArrayList<>(pendingAttachmentIds);
        clearReply();

        dmRepository.sendMessage(channelId, replyId, trimmed, attachmentIdsCopy, messageType, result -> {
            if (result.getData() != null) {
                String serverId = result.getData().getId();
                if (serverId != null) pendingServerIds.add(serverId);
                runOnUiThread(() -> {
                    DmMessageItem realItem = mapMessage(result.getData());
                    realItem.setStatus(resolvePostSendStatus(tempId));
                    adapter.replaceTempItem(tempId, realItem);
                    if (serverId != null) pendingServerIds.remove(serverId);
                    clearAttachmentPreview();
                    scrollToBottom();
                });
            } else {
                runOnUiThread(() -> {
                    adapter.removeItemById(tempId);
                    if (result.getMessage() != null) {
                        Snackbar.make(binding.getRoot(), result.getMessage(), Snackbar.LENGTH_SHORT).show();
                    }
                });
            }
        });
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
                    if (adapter.getItemCount() > 0) {
                        binding.rvMessages.scrollToPosition(adapter.getItemCount() - 1);
                    }
                    fetchPeerReadStatusIntoAdapter();
                    scheduleMarkChannelRead();
                });
            }
        });
    }

    private void sendMessage(String content) {
        sendMessage(content, null);
    }

    private void sendMessage(String content, String replyToId) {
        dmRepository.sendMessage(channelId, replyToId, content, result -> {
            if (result.getData() != null) {
                runOnUiThread(() -> appendMessage(result.getData()));
            }
        });
    }


    private void showMessageActionsSheet(DmMessageItem item) {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        BottomSheetMessageActionsBinding sheet = BottomSheetMessageActionsBinding.inflate(getLayoutInflater());
        dialog.setContentView(sheet.getRoot());

        boolean canModify = item != null && item.isMine() && !item.isDeleted();
        sheet.actionEdit.setVisibility(canModify ? View.VISIBLE : View.GONE);
        sheet.dividerEditReply.setVisibility(canModify ? View.VISIBLE : View.GONE);
        sheet.cardUnsend.setVisibility(canModify ? View.VISIBLE : View.GONE);

        View.OnClickListener reactionEmojiClick = v -> {
            String emoji = null;
            if (v == sheet.chipReactionOk) emoji = "👌";
            else if (v == sheet.chipReactionHeart) emoji = "❤️";
            else if (v == sheet.chipReactionScream) emoji = "😱";
            else if (v == sheet.chipReactionLike) emoji = "👍";
            else if (v == sheet.chipReactionMonkey) emoji = "🐒";
            else if (v == sheet.chipReactionSmile) emoji = "😊";
            if (emoji != null) {
                dialog.dismiss();
                toggleReaction(item.getId(), emoji);
            }
        };
        sheet.chipReactionOk.setOnClickListener(reactionEmojiClick);
        sheet.chipReactionHeart.setOnClickListener(reactionEmojiClick);
        sheet.chipReactionScream.setOnClickListener(reactionEmojiClick);
        sheet.chipReactionLike.setOnClickListener(reactionEmojiClick);
        sheet.chipReactionMonkey.setOnClickListener(reactionEmojiClick);
        sheet.chipReactionSmile.setOnClickListener(reactionEmojiClick);
        sheet.chipReactionMore.setOnClickListener(v -> {
            dialog.dismiss();
            showReactionPicker(item);
        });

        sheet.actionEdit.setOnClickListener(v -> { dialog.dismiss(); startEditMessage(item); });
        sheet.actionReply.setOnClickListener(v -> { dialog.dismiss(); startReply(item); });
        sheet.actionForward.setOnClickListener(v -> { dialog.dismiss(); showForwardSheet(item); });
        sheet.actionCopyText.setOnClickListener(v -> { dialog.dismiss(); copyMessageText(item); });
        sheet.actionMarkUnread.setOnClickListener(v -> {
            dialog.dismiss();
            Snackbar.make(binding.getRoot(), getString(R.string.main_coming_soon), Snackbar.LENGTH_SHORT).show();
        });
        sheet.actionReactions.setOnClickListener(v -> {
            dialog.dismiss();
            showReactionPicker(item);
        });
        sheet.actionUnsend.setOnClickListener(v -> { dialog.dismiss(); confirmUnsendMessage(item); });

        dialog.show();
    }

    private void showReactionPicker(DmMessageItem item) {
        BottomSheetDialog pickerDialog = new BottomSheetDialog(this);
        com.example.hubble.view.emoji.EmojiPickerView pickerView =
                new com.example.hubble.view.emoji.EmojiPickerView(this);
        pickerView.setOnEmojiSelectedListener(emoji -> {
            pickerDialog.dismiss();
            toggleReaction(item.getId(), emoji);
        });
        android.widget.FrameLayout container = new android.widget.FrameLayout(this);
        int height = (int) (getResources().getDisplayMetrics().heightPixels * 0.55f);
        container.addView(pickerView, new android.widget.FrameLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT, height));
        pickerDialog.setContentView(container);
        pickerDialog.show();
    }

    private void toggleReaction(String messageId, String emoji) {
        if (messageId == null || emoji == null) return;
        dmRepository.toggleReaction(messageId, emoji, result -> {
            if (result.getData() != null) {
                runOnUiThread(() -> adapter.updateReactions(messageId, result.getData()));
            }
        });
    }

    private void showForwardSheet(DmMessageItem item) {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        BottomSheetForwardMessageBinding sheet = BottomSheetForwardMessageBinding.inflate(getLayoutInflater());
        dialog.setContentView(sheet.getRoot());

        ForwardTargetAdapter forwardAdapter = new ForwardTargetAdapter(selectedIds -> sheet.btnSendForward.setEnabled(!selectedIds.isEmpty()));
        sheet.rvForwardTargets.setLayoutManager(new LinearLayoutManager(this));
        sheet.rvForwardTargets.setAdapter(forwardAdapter);
        sheet.tvForwardOriginal.setText(extractForwardPreview(item));

        sheet.btnCloseForward.setOnClickListener(v -> dialog.dismiss());
        sheet.btnCopyLink.setOnClickListener(v -> Snackbar.make(binding.getRoot(), getString(R.string.main_coming_soon), Snackbar.LENGTH_SHORT).show());

        sheet.etForwardSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) { forwardAdapter.applyFilter(s == null ? "" : s.toString()); }
        });

        dmRepository.getDirectChannels(result -> runOnUiThread(() -> {
            List<ForwardTargetAdapter.TargetItem> targets = new ArrayList<>();
            List<ChannelDto> channels = result.getData();
            if (channels != null) {
                for (ChannelDto channel : channels) {
                    if (channel.getId() == null) continue;
                    targets.add(new ForwardTargetAdapter.TargetItem(channel.getId(), buildForwardTargetTitle(channel), buildForwardTargetSubtitle(channel)));
                }
            }
            forwardAdapter.setItems(targets);
        }));

        sheet.btnSendForward.setOnClickListener(v -> {
            Set<String> targetIds = forwardAdapter.getSelectedIds();
            if (targetIds.isEmpty()) { Snackbar.make(binding.getRoot(), getString(R.string.forward_no_target), Snackbar.LENGTH_SHORT).show(); return; }
            String optionalMessage = sheet.etForwardOptionalMessage.getText() == null ? "" : sheet.etForwardOptionalMessage.getText().toString().trim();
            sendForwardMessageToChannels(targetIds, buildForwardPayload(item, optionalMessage), dialog);
        });

        dialog.show();
    }

    private String buildForwardTargetTitle(ChannelDto channel) {
        if (!TextUtils.isEmpty(channel.getPeerDisplayName())) return channel.getPeerDisplayName();
        if (!TextUtils.isEmpty(channel.getPeerUsername())) return channel.getPeerUsername();
        if (!TextUtils.isEmpty(channel.getName())) return channel.getName();
        return getString(R.string.dm_default_user);
    }

    private String buildForwardTargetSubtitle(ChannelDto channel) {
        if (!TextUtils.isEmpty(channel.getPeerUsername())) return "@" + channel.getPeerUsername();
        if (!TextUtils.isEmpty(channel.getType())) return channel.getType();
        return null;
    }

    private String extractForwardPreview(DmMessageItem item) {
        String raw = item.getContent() == null ? "" : item.getContent();
        if (DmMessageAdapter.isMedia(raw)) {
            String title = DmMessageAdapter.extractMediaTitle(raw);
            return title != null ? title : DmMessageAdapter.extractMediaUrl(raw);
        }
        return raw;
    }

    private String buildForwardPayload(DmMessageItem item, String optionalMessage) {
        String raw = item.getContent() == null ? "" : item.getContent();
        if (optionalMessage == null || optionalMessage.isEmpty()) return raw;
        return optionalMessage + "\n\n" + raw;
    }

    private void sendForwardMessageToChannels(Set<String> targetIds, String payload, BottomSheetDialog dialog) {
        final int total = targetIds.size();
        final int[] done = {0};
        final int[] success = {0};

        for (String targetChannelId : targetIds) {
            dmRepository.sendMessage(targetChannelId, null, payload, new ArrayList<>(), "TEXT", result -> runOnUiThread(() -> {
                done[0] += 1;
                if (result.getData() != null) success[0] += 1;
                if (done[0] == total) {
                    dialog.dismiss();
                    Snackbar.make(binding.getRoot(), getString(R.string.forward_sent_count, success[0]), Snackbar.LENGTH_SHORT).show();
                }
            }));
        }
    }

    private void copyMessageText(DmMessageItem item) {
        String raw = item.getContent() == null ? "" : item.getContent();
        String textToCopy = DmMessageAdapter.isMedia(raw) ? DmMessageAdapter.extractMediaUrl(raw) : raw;
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard == null) return;
        clipboard.setPrimaryClip(ClipData.newPlainText("message", textToCopy));
        showCopyBanner();
    }

    private void showCopyBanner() {
        uiHandler.removeCallbacks(hideCopyBannerRunnable);
        binding.copyBanner.animate().cancel();
        binding.copyBanner.setVisibility(View.VISIBLE);
        binding.copyBanner.setAlpha(0f);
        binding.copyBanner.setTranslationY(-12f);
        binding.copyBanner.animate().alpha(1f).translationY(0f).setDuration(180).start();
        uiHandler.postDelayed(hideCopyBannerRunnable, 1800);
    }

    private void hideCopyBanner() {
        if (binding.copyBanner.getVisibility() != View.VISIBLE) return;
        binding.copyBanner.animate().alpha(0f).translationY(-8f).setDuration(160).withEndAction(() -> {
            binding.copyBanner.setVisibility(View.GONE);
            binding.copyBanner.setAlpha(1f);
            binding.copyBanner.setTranslationY(0f);
        }).start();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // STOMP Connection (Main)
    // ─────────────────────────────────────────────────────────────────────────

    private void connectStomp() {
        if (TextUtils.isEmpty(channelId)) return;
        // Disconnect any previous client before creating a new one
        disconnectStomp();

        String wsUrl = NetworkConfig.getWebSocketUrl("ws");
        String accessToken = tokenManager.getAccessToken();
        Map<String, String> handshakeHeaders = new HashMap<>();
        List<StompHeader> connectHeaders = null;
        if (accessToken != null && !accessToken.trim().isEmpty()) {
            String authorization = "Bearer " + accessToken;
            handshakeHeaders.put("Authorization", authorization);
            connectHeaders = Collections.singletonList(new StompHeader("Authorization", authorization));
        }

        stompClient = Stomp.over(
                Stomp.ConnectionProvider.OKHTTP,
                wsUrl,
                handshakeHeaders.isEmpty() ? null : handshakeHeaders,
                createRealtimeOkHttpClient()
        );
        stompClient.withClientHeartbeat(10000).withServerHeartbeat(10000);

        final List<StompHeader> finalConnectHeaders = connectHeaders;
        disposables.add(stompClient.lifecycle()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(event -> {
                    switch (event.getType()) {
                        case OPENED:
                            subscribeToChannel();
                            // STOMP does not replay missed messages after disconnect (airplane, etc.).
                            // Always sync from REST on connect/reconnect so B sees server state and
                            // sendDeliveryAck() can notify A → "✓✓ Đã nhận".
                            loadMessageHistory();
                            sendDeliveryAck();
                            break;
                        case CLOSED:
                        case ERROR:
                            // Retry connection after a short delay on error/close
                            uiHandler.postDelayed(() -> {
                                if (!isFinishing() && !isDestroyed()) connectStomp();
                            }, 3000);
                            break;
                        default:
                            break;
                    }
                }, throwable -> {}));
        stompClient.connect(finalConnectHeaders);
    }

    private OkHttpClient createRealtimeOkHttpClient() {
        return new OkHttpClient.Builder()
                .dns(createDnsWithRailwayFallback())
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(0, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build();
    }

    private Dns createDnsWithRailwayFallback() {
        return hostname -> {
            try {
                return Dns.SYSTEM.lookup(hostname);
            } catch (UnknownHostException originalError) {
                if (!RAILWAY_HOST.equalsIgnoreCase(hostname)) {
                    throw originalError;
                }

                List<InetAddress> fallbackAddresses = new ArrayList<>();
                for (String ip : RAILWAY_FALLBACK_IPS) {
                    try {
                        fallbackAddresses.add(InetAddress.getByName(ip));
                    } catch (UnknownHostException ignored) {
                        // Ignore malformed fallback entries.
                    }
                }

                if (fallbackAddresses.isEmpty()) {
                    throw originalError;
                }
                return fallbackAddresses;
            }
        };
    }

    private void subscribeToChannel() {
        if (TextUtils.isEmpty(channelId) || stompClient == null) return;

        // Subscribe to new messages
        disposables.add(stompClient
                .topic("/topic/channels/" + channelId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(stompMessage -> {
                    MessageDto dto = parseMessagePayload(stompMessage.getPayload());
                    if (dto == null) return;
                    appendOrUpdateMessage(dto);
                }, throwable -> {}));

        // Subscribe to reaction events
        disposables.add(stompClient
                .topic("/topic/channels/" + channelId + "/reactions")
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(stompMessage -> {
                    try {
                        ReactionEventDto event = gson.fromJson(stompMessage.getPayload(), ReactionEventDto.class);
                        if (event != null && event.messageId != null && event.reactions != null) {
                            adapter.updateReactions(event.messageId, event.reactions);
                        }
                    } catch (Exception ignored) {}
                }, throwable -> {}));

        // Subscribe to typing events
        disposables.add(stompClient
                .topic("/topic/channels/" + channelId + "/typing")
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(stompMessage -> {
                    try {
                        TypingEventDto event = gson.fromJson(stompMessage.getPayload(), TypingEventDto.class);
                        if (event != null && !TextUtils.equals(event.userId, currentUserId)) {
                            showTypingIndicator(peerDisplayName);
                        }
                    } catch (Exception ignored) {}
                }, throwable -> {}));

        // Sender-side: receive delivery acks and update to "✓✓ Đã nhận".
        disposables.add(stompClient
                .topic("/topic/channels/" + channelId + "/delivery")
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(stompMessage -> {
                    try {
                        DeliveryAckDto event = gson.fromJson(stompMessage.getPayload(), DeliveryAckDto.class);
                        if (event != null && !TextUtils.equals(event.userId, currentUserId)) {
                            adapter.markAllMineDelivered();
                        }
                    } catch (Exception ignored) {}
                }, throwable -> {}));

        disposables.add(stompClient
                .topic("/topic/channels/" + channelId + "/read")
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(stompMessage -> {
                    try {
                        ReadReceiptDto event = gson.fromJson(stompMessage.getPayload(), ReadReceiptDto.class);
                        if (event != null && event.readAt != null
                                && !TextUtils.equals(event.userId, currentUserId)) {
                            long ms = parseCreatedAtMillis(event.readAt);
                            if (ms >= 0) {
                                adapter.applyPeerReadAtMillis(ms);
                            }
                        }
                    } catch (Exception ignored) {}
                }, throwable -> {}));
        disposables.add(stompClient
                .topic("/topic/channels/" + channelId + "/suggestions")
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(stompMessage -> {
                    android.util.Log.d("SmartReply", "Nhận được payload: " + stompMessage.getPayload());
                    try {
                        com.example.hubble.data.model.dm.SmartReplyResponse response =
                                gson.fromJson(stompMessage.getPayload(), com.example.hubble.data.model.dm.SmartReplyResponse.class);

                        // CHỈ HIỆN GỢI Ý NẾU TIN NHẮN KHÔNG PHẢI DO CHÍNH MÌNH GỬI
                        if (response != null) {
                            android.util.Log.d("SmartReply", "Author: " + response.getMessageAuthorId() + ", Me: " + currentUserId);
                            if (!currentUserId.equals(response.getMessageAuthorId())) {
                                runOnUiThread(() -> showSmartReplies(response.getSuggestions()));
                            }
                        }
                    } catch (Exception e) {
                        android.util.Log.e("SmartReply", "Lỗi Parse JSON từ Backend: ", e);
                    }
                }, throwable -> {
                    android.util.Log.e("SmartReply", "Lỗi đường truyền Stomp: ", throwable);
                }));
    }

    /**
     * Tells the server to broadcast delivery ack on this channel so the sender's client
     * can show "✓✓ Đã nhận". Called after (re)connect and after syncing history from REST.
     */
    private void sendDeliveryAck() {
        if (stompClient == null || TextUtils.isEmpty(channelId) || TextUtils.isEmpty(currentUserId)) return;
        String payload = "{\"userId\":\"" + currentUserId + "\"}";
        disposables.add(stompClient.send("/app/channels/" + channelId + "/delivered", payload)
                .subscribeOn(Schedulers.io())
                .subscribe(() -> {}, throwable -> {}));
    }

    private void sendTypingEvent() {
        if (stompClient == null || TextUtils.isEmpty(channelId) || TextUtils.isEmpty(currentUserId)) return;
        String payload = "{\"userId\":\"" + currentUserId + "\"}";
        disposables.add(stompClient.send("/app/channels/" + channelId + "/typing", payload)
                .subscribeOn(Schedulers.io())
                .subscribe(() -> {}, throwable -> {}));
    }

    private void showTypingIndicator(String displayName) {
        uiHandler.removeCallbacks(hideTypingRunnable);
        binding.tvTypingLabel.setText(getString(R.string.typing_indicator, displayName));
        if (binding.typingIndicatorBar.getVisibility() != View.VISIBLE) {
            binding.typingIndicatorBar.setVisibility(View.VISIBLE);
            startTypingDotsAnimation();
        }
        uiHandler.postDelayed(hideTypingRunnable, TYPING_HIDE_DELAY_MS);
    }

    private void hideTypingIndicator() {
        binding.typingIndicatorBar.setVisibility(View.GONE);
        stopTypingDotsAnimation();
    }

    private void startTypingDotsAnimation() {
        float bouncePx = 7 * getResources().getDisplayMetrics().density;
        View[] dots = {binding.typingDot1, binding.typingDot2, binding.typingDot3};
        for (int i = 0; i < 3; i++) {
            ObjectAnimator anim = ObjectAnimator.ofFloat(dots[i], "translationY", 0f, -bouncePx, 0f);
            anim.setDuration(550);
            anim.setStartDelay(i * 160L);
            anim.setRepeatCount(ObjectAnimator.INFINITE);
            anim.setRepeatMode(ObjectAnimator.RESTART);
            anim.setInterpolator(new AccelerateDecelerateInterpolator());
            dotAnimators[i] = anim;
            anim.start();
        }
    }

    private void stopTypingDotsAnimation() {
        for (ObjectAnimator anim : dotAnimators) {
            if (anim != null) anim.cancel();
        }
    }

    private static class TypingEventDto {
        String userId;
    }

    private static class ReactionEventDto {
        String messageId;
        java.util.List<ReactionDto> reactions;
    }

    private static class DeliveryAckDto {
        String userId;
    }

    private static class ReadReceiptDto {
        String userId;
        String readAt;
    }

    private void fetchPeerReadStatusIntoAdapter() {
        if (TextUtils.isEmpty(channelId)) return;
        dmRepository.getPeerReadStatus(channelId, result -> runOnUiThread(() -> {
            if (isFinishing() || isDestroyed()) return;
            if (!result.isSuccess() || result.getData() == null) return;
            String raw = result.getData().getReadAt();
            if (raw == null || raw.trim().isEmpty()) return;
            long ms = parseCreatedAtMillis(raw.trim());
            if (ms >= 0) {
                adapter.applyPeerReadAtMillis(ms);
            }
        }));
    }

    private void scheduleMarkChannelRead() {
        if (isFinishing() || isDestroyed()) return;
        uiHandler.removeCallbacks(flushMarkReadRunnable);
        uiHandler.postDelayed(flushMarkReadRunnable, 400);
    }

    private void flushMarkChannelRead() {
        if (isFinishing() || isDestroyed()) return;
        if (TextUtils.isEmpty(channelId)) return;
        String id = adapter.getLatestMessageIdForReadReceipt();
        if (id == null) return;
        if (id.equals(lastMarkedReadMessageId)) return;
        lastMarkedReadMessageId = id;
        dmRepository.markChannelRead(channelId, id, result -> { });
    }

    private MessageDto parseMessagePayload(String payload) {
        if (payload == null) return null;
        try {
            StompMessageEvent event = gson.fromJson(payload, StompMessageEvent.class);
            if (event != null && event.message != null) return event.message;
        } catch (Exception ignored) {}
        try {
            return gson.fromJson(payload, MessageDto.class);
        } catch (Exception ignored) {}
        return null;
    }

    private static class StompMessageEvent {
        String action;
        MessageDto message;
    }

    private void disconnectStomp() {
        uiHandler.removeCallbacksAndMessages(null);
        disposables.clear();
        if (stompClient != null) {
            stompClient.disconnect();
            stompClient = null;
        }
    }

    private void scrollToBottom() {
        if (adapter.getItemCount() > 0) {
            binding.rvMessages.scrollToPosition(adapter.getItemCount() - 1);
        }
    }

    /**
     * Determines the correct status for a message after the HTTP response arrives.
     * If the optimistic (temp) item was already marked DELIVERED by a delivery ack
     * that arrived faster than the HTTP response, we keep DELIVERED.
     * Otherwise, the message is confirmed as SENT (reached the server).
     */
    private DmMessageItem.MessageStatus resolvePostSendStatus(String tempId) {
        DmMessageItem temp = adapter.getItemById(tempId);
        if (temp != null && temp.getStatus() == DmMessageItem.MessageStatus.DELIVERED) {
            return DmMessageItem.MessageStatus.DELIVERED;
        }
        return DmMessageItem.MessageStatus.SENT;
    }

    /** Creates an optimistic (pre-send) DmMessageItem with SENDING status. */
    private DmMessageItem buildOptimisticItem(String tempId, String content, String type) {
        String time = formatTime(java.time.OffsetDateTime.now().toString());
        long nowMillis = System.currentTimeMillis();
        DmMessageItem item = new DmMessageItem(
                tempId, currentUserName,
                content != null ? content : "",
                time, type, nowMillis,
                true, null
        );
        item.setStatus(DmMessageItem.MessageStatus.SENDING);
        if (replyingToItem != null) {
            item.setReplyToSenderName(replyingToItem.getSenderName());
            item.setReplyToContent(replyingToItem.getContent());
        }
        return item;
    }

    private void appendMessage(MessageDto dto) {
        appendOrUpdateMessage(dto);
    }

    private void appendOrUpdateMessage(MessageDto dto) {
        if (dto == null) return;

        // If this is STOMP echo of our own message that is still in-flight (optimistic
        // temp item exists), skip it — the HTTP callback will call replaceTempItem.
        boolean mine = currentUserId != null && currentUserId.equals(dto.getAuthorId());
        if (mine && dto.getId() != null && pendingServerIds.contains(dto.getId())) {
            return;
        }

        DmMessageItem item = mapMessage(dto);

        if (mine) {
            // For our own messages arriving via STOMP (e.g. edited, or late echo):
            // preserve the highest status already set.
            DmMessageItem existing = adapter.getItemById(item.getId());
            if (existing != null && existing.getStatus() != null) {
                item.setStatus(existing.getStatus());
            } else {
                // STOMP echo of a message we sent: server confirmed it → SENT
                item.setStatus(DmMessageItem.MessageStatus.SENT);
            }
        }
        // Peer messages: status stays null (no indicator needed for received messages)

        adapter.upsertItem(item);
        scrollToBottom();
        scheduleMarkChannelRead();
    }

    private DmMessageItem mapMessage(MessageDto dto) {
        boolean mine = currentUserId != null && currentUserId.equals(dto.getAuthorId());
        String sender = mine ? currentUserName : peerDisplayName;

        // Truyền thêm danh sách attachments vào Constructor
        DmMessageItem item = new DmMessageItem(
                dto.getId(), sender,
                dto.getContent() == null ? "" : dto.getContent(),
            formatTime(dto.getCreatedAt()), dto.getType(), parseCreatedAtMillis(dto.getCreatedAt()),
            mine, dto.getAttachments()
        );
        item.setEdited(!TextUtils.isEmpty(dto.getEditedAt()));
        item.setDeleted(Boolean.TRUE.equals(dto.getIsDeleted()));
        item.setReactions(dto.getReactions());
        if (!TextUtils.isEmpty(dto.getReplyToId())) {
            DmMessageItem replyItem = adapter.getItemById(dto.getReplyToId());
            if (replyItem != null) {
                item.setReplyToSenderName(replyItem.getSenderName());
                item.setReplyToContent(replyItem.getContent());
            }
        }
        return item;
    }

    private List<DmMessageItem> mapMessages(List<MessageDto> rawMessages) {
        Map<String, MessageDto> byId = new HashMap<>();
        for (MessageDto dto : rawMessages) {
            if (!TextUtils.isEmpty(dto.getId())) byId.put(dto.getId(), dto);
        }

        List<DmMessageItem> mapped = new ArrayList<>();
        for (MessageDto dto : rawMessages) {
            if (Boolean.TRUE.equals(dto.getIsDeleted())) continue;
            boolean mine = currentUserId != null && currentUserId.equals(dto.getAuthorId());
            String sender = mine ? currentUserName : peerDisplayName;

            DmMessageItem item = new DmMessageItem(
                    dto.getId(), sender,
                    dto.getContent() == null ? "" : dto.getContent(),
                    formatTime(dto.getCreatedAt()), dto.getType(), parseCreatedAtMillis(dto.getCreatedAt()),
                    mine, dto.getAttachments()
            );
            item.setEdited(!TextUtils.isEmpty(dto.getEditedAt()));
            item.setDeleted(Boolean.TRUE.equals(dto.getIsDeleted()));
            item.setReactions(dto.getReactions());

            if (!TextUtils.isEmpty(dto.getReplyToId())) {
                MessageDto replyDto = byId.get(dto.getReplyToId());
                if (replyDto != null) {
                    boolean replyMine = currentUserId != null && currentUserId.equals(replyDto.getAuthorId());
                    item.setReplyToSenderName(replyMine ? currentUserName : peerDisplayName);
                    item.setReplyToContent(replyDto.getContent() == null ? "" : replyDto.getContent());
                }
            }
            mapped.add(item);
        }
        return mapped;
    }

    private void showSmartReplies(List<String> suggestions) {
        if (suggestions == null || suggestions.isEmpty()) {
            binding.suggestionBar.setVisibility(View.GONE);
            return;
        }

        binding.cgSuggestions.removeAllViews();
        for (String text : suggestions) {
            com.google.android.material.chip.Chip chip = new com.google.android.material.chip.Chip(this);
            chip.setText(text);

            chip.setChipBackgroundColorResource(R.color.color_surface_elevated);

            chip.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.color_text_primary));

            chip.setChipStrokeColorResource(R.color.color_divider);
            chip.setChipStrokeWidth(1f);
            chip.setOnClickListener(v -> {
                binding.etComposer.setText(text);
                attemptSendMessage();
                binding.suggestionBar.setVisibility(View.GONE);
            });

            binding.cgSuggestions.addView(chip);
        }

        binding.suggestionBar.setVisibility(View.VISIBLE);
        binding.suggestionBar.setAlpha(0f);
        binding.suggestionBar.animate().alpha(1f).setDuration(300).start();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Reply, Edit, Delete (Main)
    // ─────────────────────────────────────────────────────────────────────────

    private void startReply(DmMessageItem item) {
        clearEditMode(false);
        replyingToItem = item;
        binding.tvReplySender.setText(getString(R.string.reply_to, item.getSenderName()));
        String preview = item.getContent();
        if (DmMessageAdapter.isMedia(preview)) {
            String title = DmMessageAdapter.extractMediaTitle(preview);
            preview = title != null ? title : getString(R.string.dm_media);
        }
        binding.tvReplyContent.setText(preview);
        binding.replyBar.setVisibility(View.VISIBLE);
        binding.etComposer.requestFocus();
        showKeyboard();
    }

    private void clearReply() {
        replyingToItem = null;
        binding.replyBar.setVisibility(View.GONE);
    }

    private void startEditMessage(DmMessageItem item) {
        if (item == null || TextUtils.isEmpty(item.getId()) || item.isDeleted()) return;
        if (DmMessageAdapter.isMedia(item.getContent())) {
            Snackbar.make(binding.getRoot(), getString(R.string.main_coming_soon), Snackbar.LENGTH_SHORT).show();
            return;
        }

        clearReply();
        editingItem = item;
        binding.editBar.setVisibility(View.VISIBLE);
        binding.etComposer.setHint("");
        String content = item.getContent() == null ? "" : item.getContent();
        binding.etComposer.setText(content);
        binding.etComposer.setSelection(content.length());
        binding.etComposer.requestFocus();
        showKeyboard();
    }

    private void clearEditMode(boolean clearComposerText) {
        if (editingItem == null) return;
        editingItem = null;
        binding.editBar.setVisibility(View.GONE);
        binding.etComposer.setHint(getString(R.string.dm_message_hint, peerDisplayName));
        if (clearComposerText) binding.etComposer.setText("");
    }

    private void commitEdit(String newContent) {
        if (editingItem == null || TextUtils.isEmpty(editingItem.getId())) return;
        String editingId = editingItem.getId();
        clearEditMode(false);
        binding.etComposer.setText("");

        dmRepository.editMessage(editingId, newContent, result -> runOnUiThread(() -> {
            if (result.getData() != null) appendOrUpdateMessage(result.getData());
            else if (result.getMessage() != null) Snackbar.make(binding.getRoot(), result.getMessage(), Snackbar.LENGTH_SHORT).show();
        }));
    }

    private void confirmUnsendMessage(DmMessageItem item) {
        if (item == null || TextUtils.isEmpty(item.getId())) return;

        AlertDialog dialog = new AlertDialog.Builder(this).create();
        DialogDeleteMessageBinding deleteBinding = DialogDeleteMessageBinding.inflate(getLayoutInflater());
        dialog.setView(deleteBinding.getRoot());

        deleteBinding.tvDeleteSender.setText(item.getSenderName());
        deleteBinding.tvDeleteTime.setText(item.getTimestamp());
        String content = item.getContent() == null ? "" : item.getContent();
        if (DmMessageAdapter.isMedia(content)) {
            String title = DmMessageAdapter.extractMediaTitle(content);
            deleteBinding.tvDeleteContent.setText(title != null ? title : getString(R.string.dm_media));
        } else {
            String editedSuffix = item.isEdited()
                    ? getString(R.string.message_edited_suffix, getString(R.string.message_edited))
                    : "";
            deleteBinding.tvDeleteContent.setText(content + editedSuffix);
        }

        deleteBinding.btnDeleteNo.setOnClickListener(v -> dialog.dismiss());
        deleteBinding.btnDeleteYes.setOnClickListener(v -> {
            dialog.dismiss();
            dmRepository.unsendMessage(item.getId(), result -> runOnUiThread(() -> {
                if (result.isSuccess()) {
                    adapter.removeItemById(item.getId());
                    if (result.getData() != null) appendOrUpdateMessage(result.getData());
                } else if (result.getMessage() != null) {
                    Snackbar.make(binding.getRoot(), result.getMessage(), Snackbar.LENGTH_SHORT).show();
                }
            }));
        });

        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        dialog.show();
    }

    private String formatTime(String rawTime) {
        if (rawTime == null || rawTime.trim().isEmpty()) return "";
        String value = rawTime.trim();
        ZoneId localZone = ZoneId.systemDefault();
        try {
            OffsetDateTime dateTime = OffsetDateTime.parse(value);
            return dateTime.atZoneSameInstant(localZone)
                    .format(DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault()));
        } catch (Exception ignored) {
        }

        try {
            Instant instant = Instant.parse(value);
            return DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())
                    .format(instant.atZone(localZone));
        } catch (Exception ignored) {
        }

        try {
            LocalDateTime localDateTime = LocalDateTime.parse(value);
            // LocalDateTime values from backend have no offset, so display them as local time.
            return localDateTime.atZone(localZone)
                    .format(DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault()));
        } catch (Exception ignored) {
        }

        return value;
    }

    private long parseCreatedAtMillis(String rawTime) {
        if (rawTime == null || rawTime.trim().isEmpty()) return -1L;
        String value = rawTime.trim();
        ZoneId localZone = ZoneId.systemDefault();
        try {
            return OffsetDateTime.parse(value)
                    .atZoneSameInstant(localZone)
                    .toInstant()
                    .toEpochMilli();
        } catch (Exception ignored) {
        }

        try {
            return Instant.parse(value).toEpochMilli();
        } catch (Exception ignored) {
        }

        try {
            LocalDateTime localDateTime = LocalDateTime.parse(value);
            return localDateTime.atZone(localZone).toInstant().toEpochMilli();
        } catch (Exception ignored) {
        }

        return -1L;
    }
}
