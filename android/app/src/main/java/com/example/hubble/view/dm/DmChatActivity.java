package com.example.hubble.view.dm;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.view.KeyEvent;
import android.graphics.drawable.ColorDrawable;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.hubble.BuildConfig;
import com.example.hubble.R;
import com.example.hubble.adapter.dm.DmMessageAdapter;
import com.example.hubble.adapter.dm.ForwardTargetAdapter;
import com.example.hubble.adapter.dm.ReplySwipeCallback;
import com.example.hubble.data.model.dm.ChannelDto;
import com.example.hubble.data.model.dm.DmMessageItem;
import com.example.hubble.data.model.dm.MessageDto;
import com.example.hubble.data.model.auth.UserResponse;
import com.example.hubble.data.repository.DmRepository;
import com.example.hubble.databinding.ActivityDmChatBinding;
import com.example.hubble.databinding.BottomSheetForwardMessageBinding;
import com.example.hubble.databinding.BottomSheetMessageActionsBinding;
import com.example.hubble.databinding.DialogDeleteMessageBinding;
import com.example.hubble.utils.TokenManager;
import com.google.android.material.bottomsheet.BottomSheetDialog;
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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

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
    private DmMessageItem replyingToItem;
    private DmMessageItem editingItem;

    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    private final Runnable hideCopyBannerRunnable = this::hideCopyBanner;

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
        uiHandler.removeCallbacksAndMessages(null);
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
        adapter.setOnMessageLongClickListener((item, anchorView) -> showMessageActionsSheet(item));
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        binding.rvMessages.setLayoutManager(layoutManager);
        binding.rvMessages.setAdapter(adapter);

        ReplySwipeCallback swipeCallback = new ReplySwipeCallback(this, position -> {
            DmMessageItem item = adapter.getItem(position);
            if (item != null) {
                startReply(item);
            }
        });
        new ItemTouchHelper(swipeCallback).attachToRecyclerView(binding.rvMessages);

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
        binding.btnCancelReply.setOnClickListener(v -> clearReply());
        binding.btnCancelEdit.setOnClickListener(v -> clearEditMode(true));

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
        clearEditMode(false);
        String replyId = replyingToItem != null ? replyingToItem.getId() : null;
        sendMessage(content, replyId);
        clearReply();
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
        String trimmed = content.trim();
        if (trimmed.isEmpty() || TextUtils.isEmpty(channelId)) {
            return;
        }

        if (editingItem != null) {
            commitEdit(trimmed);
            return;
        }

        binding.etComposer.setText("");
        String replyId = replyingToItem != null ? replyingToItem.getId() : null;
        sendMessage(content, replyId);
        clearReply();
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
        sheet.actionUnsend.setVisibility(canModify ? View.VISIBLE : View.GONE);

        View.OnClickListener reactionClick = v ->
                Snackbar.make(binding.getRoot(), getString(R.string.main_coming_soon), Snackbar.LENGTH_SHORT).show();

        sheet.chipReactionOk.setOnClickListener(reactionClick);
        sheet.chipReactionLike.setOnClickListener(reactionClick);
        sheet.chipReactionTwo.setOnClickListener(reactionClick);
        sheet.chipReactionHeart.setOnClickListener(reactionClick);
        sheet.chipReactionThree.setOnClickListener(reactionClick);

        sheet.actionReply.setOnClickListener(v -> {
            dialog.dismiss();
            startReply(item);
        });

        sheet.actionForward.setOnClickListener(v -> {
            dialog.dismiss();
            showForwardSheet(item);
        });

        sheet.actionCopyText.setOnClickListener(v -> {
            dialog.dismiss();
            copyMessageText(item);
        });

        sheet.actionEdit.setOnClickListener(v -> {
            dialog.dismiss();
            startEditMessage(item);
        });

        sheet.actionUnsend.setOnClickListener(v -> {
            dialog.dismiss();
            confirmUnsendMessage(item);
        });

        sheet.actionPin.setOnClickListener(v -> {
            dialog.dismiss();
            Snackbar.make(binding.getRoot(), getString(R.string.main_coming_soon), Snackbar.LENGTH_SHORT).show();
        });

        dialog.show();
    }

    private void showForwardSheet(DmMessageItem item) {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        BottomSheetForwardMessageBinding sheet = BottomSheetForwardMessageBinding.inflate(getLayoutInflater());
        dialog.setContentView(sheet.getRoot());

        ForwardTargetAdapter forwardAdapter = new ForwardTargetAdapter(
                selectedIds -> sheet.btnSendForward.setEnabled(!selectedIds.isEmpty())
        );

        sheet.rvForwardTargets.setLayoutManager(new LinearLayoutManager(this));
        sheet.rvForwardTargets.setAdapter(forwardAdapter);
        sheet.tvForwardOriginal.setText(extractForwardPreview(item));

        sheet.btnCloseForward.setOnClickListener(v -> dialog.dismiss());
        sheet.btnCopyLink.setOnClickListener(v ->
                Snackbar.make(binding.getRoot(), getString(R.string.main_coming_soon), Snackbar.LENGTH_SHORT).show());

        sheet.etForwardSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                forwardAdapter.applyFilter(s == null ? "" : s.toString());
            }
        });

        dmRepository.getDirectChannels(result -> runOnUiThread(() -> {
            List<ForwardTargetAdapter.TargetItem> targets = new ArrayList<>();
            List<ChannelDto> channels = result.getData();
            if (channels != null) {
                for (ChannelDto channel : channels) {
                    if (channel.getId() == null) continue;
                    String title = buildForwardTargetTitle(channel);
                    String subtitle = buildForwardTargetSubtitle(channel);
                    targets.add(new ForwardTargetAdapter.TargetItem(channel.getId(), title, subtitle));
                }
            }
            forwardAdapter.setItems(targets);
        }));

        sheet.btnSendForward.setOnClickListener(v -> {
            Set<String> targetIds = forwardAdapter.getSelectedIds();
            if (targetIds.isEmpty()) {
                Snackbar.make(binding.getRoot(), getString(R.string.forward_no_target), Snackbar.LENGTH_SHORT).show();
                return;
            }

            String optionalMessage = sheet.etForwardOptionalMessage.getText() == null
                    ? ""
                    : sheet.etForwardOptionalMessage.getText().toString().trim();
            String payload = buildForwardPayload(item, optionalMessage);
            sendForwardMessageToChannels(targetIds, payload, dialog);
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
        if (optionalMessage == null || optionalMessage.isEmpty()) {
            return raw;
        }
        return optionalMessage + "\n\n" + raw;
    }

    private void sendForwardMessageToChannels(Set<String> targetIds, String payload, BottomSheetDialog dialog) {
        final int total = targetIds.size();
        final int[] done = {0};
        final int[] success = {0};

        for (String targetChannelId : targetIds) {
            dmRepository.sendMessage(targetChannelId, payload, result -> runOnUiThread(() -> {
                done[0] += 1;
                if (result.getData() != null) {
                    success[0] += 1;
                }
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
        binding.copyBanner.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(180)
                .start();
        uiHandler.postDelayed(hideCopyBannerRunnable, 1800);
    }

    private void hideCopyBanner() {
        if (binding.copyBanner.getVisibility() != View.VISIBLE) return;
        binding.copyBanner.animate()
                .alpha(0f)
                .translationY(-8f)
                .setDuration(160)
                .withEndAction(() -> {
                    binding.copyBanner.setVisibility(View.GONE);
                    binding.copyBanner.setAlpha(1f);
                    binding.copyBanner.setTranslationY(0f);
                })
                .start();
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
                    appendOrUpdateMessage(dto);
                }, throwable -> {}));
    }

    private void disconnectStomp() {
        disposables.clear();
        if (stompClient != null) stompClient.disconnect();
    }

    private void appendMessage(MessageDto dto) {
        appendOrUpdateMessage(dto);
    }

    private void appendOrUpdateMessage(MessageDto dto) {
        if (dto == null) return;
        DmMessageItem item = mapMessage(dto);
        adapter.upsertItem(item);
        if (adapter.getItemCount() > 0) {
            binding.rvMessages.scrollToPosition(adapter.getItemCount() - 1);
        }
    }

    private DmMessageItem mapMessage(MessageDto dto) {
        boolean mine = currentUserId != null && currentUserId.equals(dto.getAuthorId());
        String sender = mine ? currentUserName : peerName;
        DmMessageItem item = new DmMessageItem(
                dto.getId(), sender,
                dto.getContent() == null ? "" : dto.getContent(),
                formatTime(dto.getCreatedAt()), mine
        );
        item.setEdited(!TextUtils.isEmpty(dto.getEditedAt()));
        item.setDeleted(Boolean.TRUE.equals(dto.getIsDeleted()));
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
            if (!TextUtils.isEmpty(dto.getId())) {
                byId.put(dto.getId(), dto);
            }
        }

        List<DmMessageItem> mapped = new ArrayList<>();
        for (MessageDto dto : rawMessages) {
            if (Boolean.TRUE.equals(dto.getIsDeleted())) {
                continue;
            }
            boolean mine = currentUserId != null && currentUserId.equals(dto.getAuthorId());
            String sender = mine ? currentUserName : peerName;
            DmMessageItem item = new DmMessageItem(
                    dto.getId(), sender,
                    dto.getContent() == null ? "" : dto.getContent(),
                    formatTime(dto.getCreatedAt()), mine
            );
            item.setEdited(!TextUtils.isEmpty(dto.getEditedAt()));
            item.setDeleted(Boolean.TRUE.equals(dto.getIsDeleted()));

            if (!TextUtils.isEmpty(dto.getReplyToId())) {
                MessageDto replyDto = byId.get(dto.getReplyToId());
                if (replyDto != null) {
                    boolean replyMine = currentUserId != null && currentUserId.equals(replyDto.getAuthorId());
                    item.setReplyToSenderName(replyMine ? currentUserName : peerName);
                    item.setReplyToContent(replyDto.getContent() == null ? "" : replyDto.getContent());
                }
            }

            mapped.add(item);
        }
        return mapped;
    }

    private void startReply(DmMessageItem item) {
        clearEditMode(false);
        replyingToItem = item;
        binding.tvReplySender.setText(getString(R.string.reply_to, item.getSenderName()));
        String preview = item.getContent();
        if (DmMessageAdapter.isMedia(preview)) {
            String title = DmMessageAdapter.extractMediaTitle(preview);
            preview = title != null ? title : "Media";
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
        binding.tilComposer.setHintEnabled(false);
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
        binding.tilComposer.setHintEnabled(true);
        binding.tilComposer.setHint(getString(R.string.dm_message_hint, peerName));
        if (clearComposerText) {
            binding.etComposer.setText("");
        }
    }

    private void commitEdit(String newContent) {
        if (editingItem == null || TextUtils.isEmpty(editingItem.getId())) return;
        String editingId = editingItem.getId();
        clearEditMode(false);
        binding.etComposer.setText("");

        dmRepository.editMessage(editingId, newContent, result -> runOnUiThread(() -> {
            if (result.getData() != null) {
                appendOrUpdateMessage(result.getData());
            } else if (result.getMessage() != null) {
                Snackbar.make(binding.getRoot(), result.getMessage(), Snackbar.LENGTH_SHORT).show();
            }
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
            deleteBinding.tvDeleteContent.setText(title != null ? title : "Media");
        } else {
            deleteBinding.tvDeleteContent.setText(content + (item.isEdited() ? " (edited)" : ""));
        }

        deleteBinding.btnDeleteNo.setOnClickListener(v -> dialog.dismiss());
        deleteBinding.btnDeleteYes.setOnClickListener(v -> {
            dialog.dismiss();
            dmRepository.unsendMessage(item.getId(), result -> runOnUiThread(() -> {
                if (result.isSuccess()) {
                    adapter.removeItemById(item.getId());
                    if (result.getData() != null) {
                        appendOrUpdateMessage(result.getData());
                    }
                } else if (result.getMessage() != null) {
                    Snackbar.make(binding.getRoot(), result.getMessage(), Snackbar.LENGTH_SHORT).show();
                }
            }));
        });

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        }
        dialog.show();
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
