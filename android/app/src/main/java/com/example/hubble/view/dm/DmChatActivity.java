package com.example.hubble.view.dm;

import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.view.KeyEvent;

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

    // Minimum height ratio considered "keyboard open"
    private static final float KEYBOARD_HEIGHT_RATIO = 0.15f;
    // Default panel height when keyboard height is unknown (360dp)
    private static final int DEFAULT_PANEL_DP = 300;

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

    // Emoji/keyboard state
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

    @Override
    public void onBackPressed() {
        if (isEmojiPanelVisible) {
            hideEmojiPanel(false);
        } else {
            super.onBackPressed();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Setup
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

        // Dismiss emoji panel on message list touch
        binding.rvMessages.setOnTouchListener((v, event) -> {
            if (isEmojiPanelVisible) {
                hideEmojiPanel(false);
            }
            return false;
        });
    }

    private void setupComposer() {
        binding.btnSend.setOnClickListener(v -> attemptSendMessage());

        binding.etComposer.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN) {
                if (event.isShiftPressed()) {
                    return false;
                }
                attemptSendMessage();
                return true;
            }
            return false;
        });

        binding.btnAttach.setOnClickListener(v ->
                Snackbar.make(binding.getRoot(), getString(R.string.main_coming_soon), Snackbar.LENGTH_SHORT).show());
        binding.btnCall.setOnClickListener(v ->
                Snackbar.make(binding.getRoot(), getString(R.string.main_coming_soon), Snackbar.LENGTH_SHORT).show());
        binding.btnVideo.setOnClickListener(v ->
                Snackbar.make(binding.getRoot(), getString(R.string.main_coming_soon), Snackbar.LENGTH_SHORT).show());

        // When user taps the EditText while emoji panel is open, swap to keyboard
        binding.etComposer.setOnClickListener(v -> {
            if (isEmojiPanelVisible) {
                hideEmojiPanel(true);
            }
        });

        binding.etComposer.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus && isEmojiPanelVisible) {
                hideEmojiPanel(true);
            }
        });
    }

    private void setupEmojiPanel() {
        binding.btnEmoji.setOnClickListener(v -> toggleEmojiPanel());

        binding.emojiPickerView.setOnEmojiSelectedListener(emoji -> insertEmojiAtCursor(emoji));

        binding.emojiPickerView.setOnMediaSelectedListener(
                new com.example.hubble.view.emoji.EmojiPickerView.OnMediaSelectedListener() {
                    @Override
                    public void onGifSelected(String gifUrl, String previewUrl, String title) {
                        String content = com.example.hubble.adapter.dm.DmMessageAdapter.GIF_PREFIX
                                + encodeMediaContent(title, gifUrl);
                        sendMediaMessage(content);
                    }

                    @Override
                    public void onStickerSelected(String stickerUrl, String previewUrl, String title) {
                        String content = com.example.hubble.adapter.dm.DmMessageAdapter.STICKER_PREFIX
                                + encodeMediaContent(title, stickerUrl);
                        sendMediaMessage(content);
                    }
                });
    }

    private void sendMediaMessage(String content) {
        if (TextUtils.isEmpty(channelId)) return;
        hideEmojiPanel(false);
        sendMessage(content);
    }

    /**
     * Encodes a media title + URL into a single string: "title\nurl".
     * If title is null/empty, returns just the URL for backward compatibility.
     */
    private static String encodeMediaContent(String title, String url) {
        if (title != null && !title.trim().isEmpty()) {
            return title.trim() + "\n" + url;
        }
        return url;
    }

    /**
     * Detects keyboard height by monitoring root view layout changes.
     * When the soft keyboard appears, the root view's visible frame shrinks.
     */
    private void setupKeyboardHeightDetection() {
        binding.getRoot().getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        Rect visibleFrame = new Rect();
                        binding.getRoot().getWindowVisibleDisplayFrame(visibleFrame);
                        int screenHeight = binding.getRoot().getRootView().getHeight();
                        int detectedKeyboardHeight = screenHeight - visibleFrame.bottom;

                        if (detectedKeyboardHeight > screenHeight * KEYBOARD_HEIGHT_RATIO) {
                            // Keyboard is visible — update our stored height
                            if (detectedKeyboardHeight != keyboardHeight) {
                                keyboardHeight = detectedKeyboardHeight;
                                // If emoji panel needs resize to match new keyboard height
                                if (isEmojiPanelVisible) {
                                    setPanelHeight(keyboardHeight);
                                }
                            }
                            // Keyboard appeared → close emoji panel silently
                            if (isEmojiPanelVisible && !pendingShowKeyboard) {
                                // User opened keyboard manually (tapped EditText etc.)
                                collapseEmojiPanel();
                            }
                            pendingShowKeyboard = false;
                        }
                    }
                });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Emoji / Keyboard switching
    // ─────────────────────────────────────────────────────────────────────────

    private void toggleEmojiPanel() {
        if (isEmojiPanelVisible) {
            // Switch back to keyboard
            hideEmojiPanel(true);
        } else {
            showEmojiPanel();
        }
    }

    private void showEmojiPanel() {
        isEmojiPanelVisible = true;
        binding.btnEmoji.setIconResource(R.drawable.ic_keyboard_open);

        int height = resolveEmojiPanelHeight();

        // Hide soft keyboard first
        hideKeyboard();

        // Set container height and make it visible
        setPanelHeight(height);
        binding.emojiPickerContainer.setVisibility(View.VISIBLE);

        // Slide up animation
        binding.emojiPickerContainer.setTranslationY(height);
        binding.emojiPickerContainer.animate()
                .translationY(0)
                .setDuration(220)
                .setInterpolator(new DecelerateInterpolator(1.5f))
                .start();
    }

    /**
     * @param showKeyboard if true, reveals keyboard after hiding panel
     */
    private void hideEmojiPanel(boolean showKeyboard) {
        if (!isEmojiPanelVisible) return;
        collapseEmojiPanel();
        if (showKeyboard) {
            pendingShowKeyboard = true;
            binding.etComposer.requestFocus();
            showKeyboard();
        }
    }

    /** Just collapses the panel without touching the keyboard */
    private void collapseEmojiPanel() {
        isEmojiPanelVisible = false;
        binding.btnEmoji.setIconResource(R.drawable.ic_emoji);
        int height = resolveEmojiPanelHeight();
        binding.emojiPickerContainer.animate()
                .translationY(height)
                .setDuration(180)
                .setInterpolator(new DecelerateInterpolator(1.5f))
                .withEndAction(() -> {
                    binding.emojiPickerContainer.setVisibility(View.GONE);
                    binding.emojiPickerContainer.setTranslationY(0);
                    setPanelHeight(0);
                })
                .start();
    }

    private void setPanelHeight(int height) {
        ViewGroup.LayoutParams params = binding.emojiPickerContainer.getLayoutParams();
        params.height = height;
        binding.emojiPickerContainer.setLayoutParams(params);
    }

    private int resolveEmojiPanelHeight() {
        if (keyboardHeight > 0) return keyboardHeight;
        return (int) (DEFAULT_PANEL_DP * getResources().getDisplayMetrics().density);
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        View focusedView = getCurrentFocus();
        if (focusedView != null && imm != null) {
            imm.hideSoftInputFromWindow(focusedView.getWindowToken(), 0);
        }
    }

    private void showKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.showSoftInput(binding.etComposer, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Emoji insertion — inserts at cursor position in the EditText
    // ─────────────────────────────────────────────────────────────────────────

    private void insertEmojiAtCursor(String emoji) {
        Editable editable = binding.etComposer.getText();
        if (editable == null) {
            binding.etComposer.setText(emoji);
            return;
        }
        int start = Math.max(binding.etComposer.getSelectionStart(), 0);
        int end = Math.max(binding.etComposer.getSelectionEnd(), 0);
        if (start > end) {
            int tmp = start; start = end; end = tmp;
        }
        editable.replace(start, end, emoji);
        // Move cursor to after inserted emoji
        binding.etComposer.setSelection(start + emoji.length());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Messaging
    // ─────────────────────────────────────────────────────────────────────────

    private void attemptSendMessage() {
        String content = binding.etComposer.getText() == null
                ? "" : binding.etComposer.getText().toString();
        if (content.trim().isEmpty() || TextUtils.isEmpty(channelId)) {
            return;
        }
        binding.etComposer.setText("");
        sendMessage(content);
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
                + "ws";

        stompClient = Stomp.over(Stomp.ConnectionProvider.OKHTTP, wsUrl);

        disposables.add(stompClient.lifecycle()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(event -> {
                    if (event.getType() == ua.naiksoftware.stomp.dto.LifecycleEvent.Type.OPENED) {
                        subscribeToChannel();
                    }
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
                    boolean isFromMe = currentUserId != null && currentUserId.equals(dto.getAuthorId());
                    if (!isFromMe) appendMessage(dto);
                }, throwable -> {}));
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
