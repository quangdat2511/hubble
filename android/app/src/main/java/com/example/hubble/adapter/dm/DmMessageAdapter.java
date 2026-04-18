package com.example.hubble.adapter.dm;

import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.hubble.R;
import com.example.hubble.data.api.NetworkConfig;
import com.example.hubble.data.model.dm.AttachmentResponse;
import com.example.hubble.data.model.dm.DmMessageItem;
import com.example.hubble.data.model.dm.ReactionDto;
import com.example.hubble.databinding.ItemDmDateSeparatorBinding;
import com.example.hubble.databinding.ItemDmMessageBinding;
import com.example.hubble.databinding.ItemChannelWelcomeIntroBinding;
import com.example.hubble.databinding.ItemDmProfileIntroBinding;
import com.example.hubble.utils.AudioProximityManager;
import com.google.android.material.chip.Chip;
import com.example.hubble.utils.AvatarPlaceholderUtils;
import com.example.hubble.utils.InAppMessageUtils;
import com.example.hubble.utils.LocalizedTimeUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class DmMessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public static final String GIF_PREFIX = "{gif}";
    public static final String STICKER_PREFIX = "{sticker}";

    private static final int TYPE_INTRO = 0;
    private static final int TYPE_DATE = 1;
    private static final int TYPE_MESSAGE = 2;
    private static final int TYPE_INTRO_CHANNEL = 3;
    private static final long GROUPING_TIME_THRESHOLD_MILLIS = 7 * 60 * 1000L;

    @NonNull
    private final Context context;
    private final List<DmMessageItem> items = new ArrayList<>();

    @Nullable
    private DmMessageItem introItem = null;

    @Nullable
    private String currentUserAvatarUrl;
    @Nullable
    private String peerAvatarUrl;
    @Nullable
    private OnMessageLongClickListener onMessageLongClickListener;
    @Nullable
    private OnReactionClickListener onReactionClickListener;
    @Nullable
    private String currentUserId;

    /** Peer's last_read boundary (server time), millis; -1 = unknown */
    private long peerLastReadAtMillis = -1L;

    /**
     * When false (e.g. server text channel), outbound rows do not show sending/sent/delivered/read.
     */
    private boolean showMineMessageStatus = true;

    private static MediaPlayer currentMediaPlayer;
    private static AudioProximityManager currentProximityManager;
    private static ImageView currentPlayButton;
    private static Handler audioHandler = new Handler(Looper.getMainLooper());
    private static Runnable updateSeekBarRunnable;
    private AudioProximityManager proximityManager;

    public DmMessageAdapter(@NonNull Context context) {
        this.context = context;
    }

    public interface OnMessageLongClickListener {
        void onMessageLongClick(@NonNull DmMessageItem item, @NonNull View anchorView);
    }

    public interface OnReactionClickListener {
        void onReactionClick(@NonNull DmMessageItem item, @NonNull String emoji);
    }

    public static boolean isGif(String content) {
        return content != null && content.startsWith(GIF_PREFIX);
    }

    public static boolean isSticker(String content) {
        return content != null && content.startsWith(STICKER_PREFIX);
    }

    public static boolean isMedia(String content) {
        return isGif(content) || isSticker(content);
    }

    public static String extractMediaUrl(String content) {
        String body = null;
        if (isGif(content)) body = content.substring(GIF_PREFIX.length());
        else if (isSticker(content)) body = content.substring(STICKER_PREFIX.length());
        else return content;

        int nl = body.indexOf('\n');
        return nl >= 0 ? body.substring(nl + 1) : body;
    }

    public static String extractMediaTitle(String content) {
        String body = null;
        if (isGif(content)) body = content.substring(GIF_PREFIX.length());
        else if (isSticker(content)) body = content.substring(STICKER_PREFIX.length());
        else return null;

        int nl = body.indexOf('\n');
        if (nl > 0) {
            String title = body.substring(0, nl).trim();
            return title.isEmpty() ? null : title;
        }
        return null;
    }

    public void setItems(List<DmMessageItem> newItems) {
        peerLastReadAtMillis = -1L;
        items.clear();
        if (newItems != null) {
            String lastDateKey = null;
            for (DmMessageItem item : newItems) {
                if (item == null || item.isDeleted()) continue;
                String dateKey = getDateKey(item.getCreatedAtMillis());
                if (dateKey != null && !dateKey.equals(lastDateKey)) {
                    items.add(DmMessageItem.createDateSeparator(
                            LocalizedTimeUtils.formatConversationDateLabel(
                                    context,
                                    item.getCreatedAtMillis()
                            )
                    ));
                    lastDateKey = dateKey;
                }
                items.add(item);
            }
        }
        notifyDataSetChanged();
    }

    private static String getDateKey(long millis) {
        if (millis < 0) return null;
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(millis);
        return cal.get(Calendar.YEAR) + "-" + cal.get(Calendar.DAY_OF_YEAR);
    }

    public void appendItem(DmMessageItem item) {
        if (item == null || item.isDeleted()) return;

        // Before inserting: record the current last-mine index so we can rebind it
        // after the new item is added (it will no longer be "last mine" and must hide
        // its status indicator, which RecyclerView won't do automatically).
        int prevLastMineIdx = -1;
        if (item.isMine()) {
            for (int i = items.size() - 1; i >= 0; i--) {
                DmMessageItem prev = items.get(i);
                if (!prev.isDateSeparator() && !prev.isIntro() && prev.isMine()) {
                    prevLastMineIdx = i;
                    break;
                }
            }
        }

        String newDateKey = getDateKey(item.getCreatedAtMillis());
        if (newDateKey != null) {
            String lastDateKey = null;
            for (int i = items.size() - 1; i >= 0; i--) {
                DmMessageItem prev = items.get(i);
                if (!prev.isDateSeparator()) {
                    lastDateKey = getDateKey(prev.getCreatedAtMillis());
                    break;
                }
            }
            if (!newDateKey.equals(lastDateKey)) {
                items.add(DmMessageItem.createDateSeparator(
                        LocalizedTimeUtils.formatConversationDateLabel(
                                context,
                                item.getCreatedAtMillis()
                        )
                ));
                notifyItemInserted(items.size() - 1 + introOffset());
            }
        }
        items.add(item);
        notifyItemInserted(items.size() - 1 + introOffset());

        // Force-rebind the previous last-mine so it hides its status indicator.
        // Without this, RecyclerView reuses the cached ViewHolder which still has
        // tvStatus visible from when that item was last mine.
        if (prevLastMineIdx >= 0) {
            notifyItemChanged(prevLastMineIdx + introOffset());
        }
    }

    public void upsertItem(DmMessageItem item) {
        if (item == null || item.getId() == null) return;
        for (int i = 0; i < items.size(); i++) {
            DmMessageItem old = items.get(i);
            if (old.isDateSeparator()) continue;
            if (item.getId().equals(old.getId())) {
                if (item.isDeleted()) {
                    // If deleted item was last mine, the new last mine must be rebound
                    boolean wasLastMine = old.isMine() && isLastMineMessageAt(i);
                    items.remove(i);
                    notifyItemRemoved(i + introOffset());
                    if (wasLastMine) {
                        // Find new last mine and notify it
                        for (int j = items.size() - 1; j >= 0; j--) {
                            DmMessageItem prev = items.get(j);
                            if (!prev.isDateSeparator() && !prev.isIntro() && prev.isMine()) {
                                notifyItemChanged(j + introOffset());
                                break;
                            }
                        }
                    }
                } else {
                    items.set(i, item);
                    notifyItemChanged(i + introOffset());
                }
                return;
            }
        }
        if (!item.isDeleted()) {
            appendItem(item);
        }
    }

    public void setIntroItem(@Nullable DmMessageItem intro) {
        this.introItem = intro;
        notifyDataSetChanged();
    }

    /** Offset to convert items-list index → adapter position. */
    private int introOffset() {
        return introItem != null ? 1 : 0;
    }

    public void setOnMessageLongClickListener(@Nullable OnMessageLongClickListener listener) {
        this.onMessageLongClickListener = listener;
    }

    public void setOnReactionClickListener(@Nullable OnReactionClickListener listener) {
        this.onReactionClickListener = listener;
    }

    public void setCurrentUserId(@Nullable String userId) {
        this.currentUserId = userId;
    }

    public void setParticipantAvatarUrls(@Nullable String currentUserAvatarUrl, @Nullable String peerAvatarUrl) {
        this.currentUserAvatarUrl = currentUserAvatarUrl;
        this.peerAvatarUrl = peerAvatarUrl;
        notifyDataSetChanged();
    }

    public void setShowMineMessageStatus(boolean show) {
        if (this.showMineMessageStatus == show) return;
        this.showMineMessageStatus = show;
        notifyDataSetChanged();
    }

    @Nullable
    public DmMessageItem getItem(int adapterPosition) {
        if (introItem != null) {
            if (adapterPosition == 0) return introItem;
            adapterPosition--;
        }
        if (adapterPosition < 0 || adapterPosition >= items.size()) return null;
        return items.get(adapterPosition);
    }

    @Nullable
    public DmMessageItem getItemById(String id) {
        if (id == null) return null;
        for (DmMessageItem item : items) {
            if (!item.isDateSeparator() && id.equals(item.getId())) return item;
        }
        return null;
    }

    public void updateReactions(@NonNull String messageId, @NonNull List<ReactionDto> reactions) {
        for (int i = 0; i < items.size(); i++) {
            DmMessageItem item = items.get(i);
            if (item.isDateSeparator()) continue;
            if (messageId.equals(item.getId())) {
                item.setReactions(reactions);
                notifyItemChanged(i + introOffset());
                return;
            }
        }
    }

    public void removeItemById(@Nullable String id) {
        if (id == null) return;
        for (int i = 0; i < items.size(); i++) {
            DmMessageItem item = items.get(i);
            if (item.isDateSeparator()) continue;
            if (id.equals(item.getId())) {
                boolean wasLastMine = item.isMine() && isLastMineMessageAt(i);
                items.remove(i);
                notifyItemRemoved(i + introOffset());
                // If the removed item was last mine, rebind the new last mine
                if (wasLastMine) {
                    for (int j = items.size() - 1; j >= 0; j--) {
                        DmMessageItem prev = items.get(j);
                        if (!prev.isDateSeparator() && !prev.isIntro() && prev.isMine()) {
                            notifyItemChanged(j + introOffset());
                            break;
                        }
                    }
                }
                return;
            }
        }
    }

    /**
     * Removes the optimistic temp item (by tempId) and upserts the confirmed
     * real item. Handles the STOMP echo race: if the real item was already
     * inserted via STOMP, upsertItem will update it in place.
     */
    public void replaceTempItem(@NonNull String tempId, @NonNull DmMessageItem realItem) {
        removeItemById(tempId);
        upsertItem(realItem);
    }

    /**
     * Marks all in-flight mine messages (SENDING or SENT) as DELIVERED.
     * Called when the peer's device receives messages (delivery ack via STOMP).
     */
    public void markAllMineDelivered() {
        int lastMineIdx = -1;
        for (int i = 0; i < items.size(); i++) {
            DmMessageItem item = items.get(i);
            if (item.isDateSeparator() || item.isIntro()) continue;
            if (item.isMine()) {
                DmMessageItem.MessageStatus s = item.getStatus();
                if (s == DmMessageItem.MessageStatus.SENDING || s == DmMessageItem.MessageStatus.SENT) {
                    item.setStatus(DmMessageItem.MessageStatus.DELIVERED);
                    lastMineIdx = i;
                }
            }
        }
        if (lastMineIdx >= 0) {
            notifyItemChanged(lastMineIdx + introOffset());
        }
    }

    /**
     * Updates the peer read-receipt boundary: messages at or before this time may show "Đã xem".
     */
    public void applyPeerReadAtMillis(long millis) {
        if (millis < 0) return;
        if (millis <= peerLastReadAtMillis) return;
        peerLastReadAtMillis = millis;
        notifyLastMineMessageChanged();
    }

    /**
     * Latest chat message id (by list order) suitable for mark-read; skips temp optimistic ids.
     */
    @Nullable
    public String getLatestMessageIdForReadReceipt() {
        for (int i = items.size() - 1; i >= 0; i--) {
            DmMessageItem item = items.get(i);
            if (item.isDateSeparator() || item.isIntro() || item.isDeleted()) continue;
            String id = item.getId();
            if (id == null || id.startsWith("tmp_")) continue;
            return id;
        }
        return null;
    }

    private void notifyLastMineMessageChanged() {
        for (int i = items.size() - 1; i >= 0; i--) {
            DmMessageItem item = items.get(i);
            if (!item.isDateSeparator() && !item.isIntro() && item.isMine()) {
                notifyItemChanged(i + introOffset());
                return;
            }
        }
    }

    /** Updates the status of a single message by id. */
    public void updateMessageStatus(@NonNull String id, @NonNull DmMessageItem.MessageStatus status) {
        for (int i = 0; i < items.size(); i++) {
            DmMessageItem item = items.get(i);
            if (item.isDateSeparator()) continue;
            if (id.equals(item.getId())) {
                item.setStatus(status);
                notifyItemChanged(i + introOffset());
                return;
            }
        }
    }

    @Override
    public int getItemCount() {
        return introOffset() + items.size();
    }

    @Override
    public int getItemViewType(int position) {
        if (introItem != null && position == 0) {
            return introItem.isChannelWelcomeIntro() ? TYPE_INTRO_CHANNEL : TYPE_INTRO;
        }
        DmMessageItem item = items.get(position - introOffset());
        if (item.isDateSeparator()) return TYPE_DATE;
        return TYPE_MESSAGE;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_INTRO) {
            return new IntroHolder(ItemDmProfileIntroBinding.inflate(inflater, parent, false));
        }
        if (viewType == TYPE_INTRO_CHANNEL) {
            return new ChannelWelcomeIntroHolder(ItemChannelWelcomeIntroBinding.inflate(inflater, parent, false));
        }
        if (viewType == TYPE_DATE) {
            return new DateSeparatorHolder(ItemDmDateSeparatorBinding.inflate(inflater, parent, false));
        }
        return new MessageRowHolder(ItemDmMessageBinding.inflate(inflater, parent, false),
                onMessageLongClickListener, onReactionClickListener);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof IntroHolder) {
            ((IntroHolder) holder).bind(introItem, peerAvatarUrl);
            return;
        }
        if (holder instanceof ChannelWelcomeIntroHolder) {
            ((ChannelWelcomeIntroHolder) holder).bind(introItem);
            return;
        }
        int rawPos = position - introOffset();
        DmMessageItem item = items.get(rawPos);
        if (holder instanceof DateSeparatorHolder) {
            ((DateSeparatorHolder) holder).bind(item.getContent());
            return;
        }
        DmMessageItem previous = rawPos > 0 ? items.get(rawPos - 1) : null;
        if (previous != null && previous.isDateSeparator()) previous = null;
        boolean groupedWithPrevious = shouldGroupWithPrevious(item, previous);
        boolean isLastMine = item.isMine() && isLastMineMessageAt(rawPos);
        String rowAvatarUrl = item.isMine() ? currentUserAvatarUrl : peerAvatarUrl;
        String replyAvatarUrl = item.hasReply()
                ? (item.isReplyToMine() ? currentUserAvatarUrl : peerAvatarUrl)
                : null;

        if (holder instanceof MessageRowHolder) {
            ((MessageRowHolder) holder).bind(item, !groupedWithPrevious, rowAvatarUrl,
                    replyAvatarUrl, currentUserId, isLastMine, peerLastReadAtMillis, showMineMessageStatus);
        }
    }

    /** Returns true if no mine message exists after rawPos in items list. */
    private boolean isLastMineMessageAt(int rawPos) {
        for (int i = rawPos + 1; i < items.size(); i++) {
            DmMessageItem next = items.get(i);
            if (!next.isDateSeparator() && !next.isIntro() && next.isMine()) {
                return false;
            }
        }
        return true;
    }

    private boolean shouldGroupWithPrevious(@Nullable DmMessageItem current, @Nullable DmMessageItem previous) {
        if (current == null || previous == null) return false;
        if (!isGroupableUserMessage(current) || !isGroupableUserMessage(previous)) return false;
        if (current.isMine() != previous.isMine()) return false;
        if (!safeEquals(current.getSenderName(), previous.getSenderName())) return false;

        long currentTime = current.getCreatedAtMillis();
        long previousTime = previous.getCreatedAtMillis();
        if (currentTime < 0 || previousTime < 0 || currentTime < previousTime) return false;
        return currentTime - previousTime <= GROUPING_TIME_THRESHOLD_MILLIS;
    }

    private boolean isGroupableUserMessage(@Nullable DmMessageItem item) {
        return item != null && !item.isDeleted() && !item.isSystemMessage() && !item.isDateSeparator();
    }

    private static boolean safeEquals(@Nullable String a, @Nullable String b) {
        if (a == null) return b == null;
        return a.equals(b);
    }

    private static void loadAttachments(LinearLayout container, List<AttachmentResponse> attachments) {
        container.removeAllViews();

        if (attachments == null || attachments.isEmpty()) {
            container.setVisibility(View.GONE);
            return;
        }

        LayoutInflater inflater = LayoutInflater.from(container.getContext());

        for (AttachmentResponse att : attachments) {
            String mimeType = att.getContentType() != null ? att.getContentType().toLowerCase() : "";
            String url = att.getUrl() == null ? "" : NetworkConfig.resolveUrl(att.getUrl());

            if (mimeType.startsWith("image/") || mimeType.startsWith("video/")) {
                View mediaView = inflater.inflate(R.layout.item_attachment_media, container, false);
                ImageView ivMedia = mediaView.findViewById(R.id.ivMedia);
                ImageView ivPlayIcon = mediaView.findViewById(R.id.ivPlayIcon);

                ivPlayIcon.setVisibility(mimeType.startsWith("video/") ? View.VISIBLE : View.GONE);

                Glide.with(container.getContext())
                        .load(url)
                        .centerCrop()
                        .placeholder(android.R.drawable.ic_menu_gallery)
                        .error(android.R.drawable.ic_menu_report_image)
                        .into(ivMedia);

//                mediaView.setOnClickListener(v -> openAttachment(container.getContext(), url, mimeType));
                mediaView.setOnClickListener(v -> {
                    if (mimeType.startsWith("image/")) {
                        // Nếu là ảnh -> Mở giao diện xem ảnh đen thui siêu xịn
                        Intent intent = new Intent(container.getContext(), com.example.hubble.view.dm.ImageViewerActivity.class);
                        intent.putExtra(com.example.hubble.view.dm.ImageViewerActivity.EXTRA_IMAGE_URL, url);
                        intent.putExtra(com.example.hubble.view.dm.ImageViewerActivity.EXTRA_FILE_NAME, att.getFilename());
                        container.getContext().startActivity(intent);
                    } else {
                        // Nếu là video -> Vẫn dùng trình duyệt ngoài như cũ
                        openAttachment(container.getContext(), url, mimeType);
                    }
                });
                container.addView(mediaView);

            } else if (mimeType.startsWith("audio/") || mimeType.endsWith("m4a") || mimeType.contains("mp4")) {
                View voiceView = inflater.inflate(R.layout.item_attachment_voice, container, false);
                ImageView btnPlayPause = voiceView.findViewById(R.id.btnPlayPause);
                SeekBar seekBarVoice = voiceView.findViewById(R.id.seekBarVoice);
                TextView tvDuration = voiceView.findViewById(R.id.tvDuration);

                btnPlayPause.setOnClickListener(v -> playAudio(url, btnPlayPause, seekBarVoice, tvDuration));
                container.addView(voiceView);

            } else {
                View fileView = inflater.inflate(R.layout.item_attachment_file, container, false);
                TextView tvFileName = fileView.findViewById(R.id.tvFileName);
                ImageView ivFileIcon = fileView.findViewById(R.id.ivFileIcon);
                TextView tvFileType = fileView.findViewById(R.id.tvFileType);
                ImageView ivSaveIcon = fileView.findViewById(R.id.ivSaveIcon);
                Context context = container.getContext();
                String fileName = att.getFilename() != null
                        ? att.getFilename()
                        : context.getString(R.string.dm_untitled_file);

                String safeFileName = fileName;
                if (safeFileName.contains("/")) safeFileName = safeFileName.substring(safeFileName.lastIndexOf("/") + 1);
                if (safeFileName.contains(":")) safeFileName = safeFileName.substring(safeFileName.lastIndexOf(":") + 1);
                safeFileName = safeFileName.replaceAll("[\\\\/:*?\"<>|]", "_");

                tvFileName.setText(safeFileName);

                String lowerMime = mimeType.toLowerCase();
                String lowerName = fileName.toLowerCase();

                if (lowerMime.contains("pdf") || lowerName.endsWith(".pdf")) {
                    tvFileType.setText(R.string.dm_file_type_pdf);
                    ivFileIcon.setImageResource(R.drawable.ic_file_pdf);
                }
                else if (lowerMime.contains("word") || lowerMime.contains("document") || lowerName.endsWith(".docx") || lowerName.endsWith(".doc")) {
                    tvFileType.setText(R.string.dm_file_type_word);
                    ivFileIcon.setImageResource(R.drawable.ic_file_docx);
                }
                else if (lowerMime.contains("excel") || lowerMime.contains("spreadsheet") || lowerName.endsWith(".xlsx") || lowerName.endsWith(".xls")) {
                    tvFileType.setText(R.string.dm_file_type_excel);
                    ivFileIcon.setImageResource(R.drawable.ic_file_excel);
                }
                else if (lowerMime.contains("powerpoint") || lowerMime.contains("presentation") || lowerName.endsWith(".pptx") || lowerName.endsWith(".ppt")) {
                    tvFileType.setText(R.string.dm_file_type_presentation);
                    ivFileIcon.setImageResource(R.drawable.ic_file_powerpoint);
                }
                else if (lowerMime.contains("zip") || lowerMime.contains("rar") || lowerName.endsWith(".zip") || lowerName.endsWith(".rar")) {
                    tvFileType.setText(R.string.dm_file_type_archive);
                    ivFileIcon.setImageResource(R.drawable.ic_file_zip);
                }
                else if (lowerMime.startsWith("text/") || lowerName.endsWith(".txt")) {
                    tvFileType.setText(R.string.dm_file_type_text);
                    ivFileIcon.setImageResource(R.drawable.ic_file_text);
                }
                else {
                    tvFileType.setText(R.string.attachment_file);
                    ivFileIcon.setImageResource(R.drawable.ic_file_generic);
                }

                ivSaveIcon.setOnClickListener(v -> {
                    downloadFile(container.getContext(), url, fileName);
                });

                fileView.setOnClickListener(v -> openAttachment(container.getContext(), url, mimeType));
                container.addView(fileView);
            }
        }
        container.setVisibility(View.VISIBLE);
    }

    private static void playAudio(String url, ImageView btnPlayPause, SeekBar seekBar, TextView tvDuration) {
        try {
            if (currentProximityManager == null) {
                currentProximityManager = new com.example.hubble.utils.AudioProximityManager(
                        btnPlayPause.getContext().getApplicationContext());
            }
            if (currentMediaPlayer != null && currentPlayButton == btnPlayPause) {
                if (currentMediaPlayer.isPlaying()) {
                    currentMediaPlayer.pause();
                    btnPlayPause.setImageResource(android.R.drawable.ic_media_play);
                    currentProximityManager.stop();
                } else {
                    currentMediaPlayer.start();
                    btnPlayPause.setImageResource(android.R.drawable.ic_media_pause);
                    audioHandler.post(updateSeekBarRunnable);
                    currentProximityManager.start();
                }
                return;
            }

            if (currentMediaPlayer != null) {
                currentMediaPlayer.stop();
                currentMediaPlayer.release();
                if (currentPlayButton != null) {
                    currentPlayButton.setImageResource(android.R.drawable.ic_media_play);
                }
                audioHandler.removeCallbacks(updateSeekBarRunnable);
                currentProximityManager.stop();
            }

            currentMediaPlayer = new MediaPlayer();
            currentPlayButton = btnPlayPause;
            currentMediaPlayer.setDataSource(url);
            currentMediaPlayer.setAudioStreamType(AudioManager.STREAM_VOICE_CALL);
            currentMediaPlayer.prepareAsync();
            btnPlayPause.setImageResource(android.R.drawable.ic_popup_sync);

            currentMediaPlayer.setOnPreparedListener(mp -> {
                seekBar.setMax(mp.getDuration());
                mp.start();
                btnPlayPause.setImageResource(android.R.drawable.ic_media_pause);
                currentProximityManager.start();

                updateSeekBarRunnable = new Runnable() {
                    @Override
                    public void run() {
                        if (currentMediaPlayer != null && currentMediaPlayer.isPlaying()) {
                            seekBar.setProgress(currentMediaPlayer.getCurrentPosition());
                            int sec = currentMediaPlayer.getCurrentPosition() / 1000;
                            tvDuration.setText(String.format("%d:%02d", sec / 60, sec % 60));
                            audioHandler.postDelayed(this, 100);
                        }
                    }
                };
                audioHandler.post(updateSeekBarRunnable);
            });

            currentMediaPlayer.setOnCompletionListener(mp -> {
                btnPlayPause.setImageResource(android.R.drawable.ic_media_play);
                seekBar.setProgress(0);
                tvDuration.setText("0:00");
                audioHandler.removeCallbacks(updateSeekBarRunnable);
                currentProximityManager.stop();
            });

            seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser && currentMediaPlayer != null) {
                        currentMediaPlayer.seekTo(progress);
                    }
                }
                @Override public void onStartTrackingTouch(SeekBar seekBar) {}
                @Override public void onStopTrackingTouch(SeekBar seekBar) {}
            });

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void releaseAudio() {
        // 1. Tắt và dọn dẹp Trình phát nhạc
        if (currentMediaPlayer != null) {
            if (currentMediaPlayer.isPlaying()) {
                currentMediaPlayer.stop();
            }
            currentMediaPlayer.release();
            currentMediaPlayer = null; // Gán bằng null để giải phóng RAM
        }

        // 2. Dọn dẹp luồng chạy ngầm của thanh thời gian (SeekBar)
        if (audioHandler != null && updateSeekBarRunnable != null) {
            audioHandler.removeCallbacks(updateSeekBarRunnable);
        }

        // 3. TẮT CẢM BIẾN TIỆM CẬN (Cực kỳ quan trọng để không bị lỗi đen màn hình)
        if (currentProximityManager != null) {
            currentProximityManager.stop();
            currentProximityManager = null; // Giải phóng Context
        }

        // 4. Reset nút Play
        currentPlayButton = null;
    }

    private static void openAttachment(Context context, String url, String mimeType) {
        if (url == null || url.isEmpty()) return;
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.parse(url), mimeType);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            try {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                browserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(browserIntent);
            } catch (Exception ex) {
                InAppMessageUtils.show(context, context.getString(R.string.dm_open_file_error));
            }
        }
    }

    private static void downloadFile(Context context, String url, String fileName) {
        if (url == null || url.isEmpty()) return;

        String finalUrl = NetworkConfig.resolveUrl(url);

        String safeFileName = fileName;
        if (safeFileName.contains("/")) safeFileName = safeFileName.substring(safeFileName.lastIndexOf("/") + 1);
        if (safeFileName.contains(":")) safeFileName = safeFileName.substring(safeFileName.lastIndexOf(":") + 1);
        safeFileName = safeFileName.replaceAll("[\\\\/:*?\"<>|]", "_");

        try {
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(finalUrl));
            request.setTitle(safeFileName);
            request.setDescription(context.getString(R.string.dm_download_description));
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);

            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, safeFileName);

            com.example.hubble.utils.TokenManager tokenManager = new com.example.hubble.utils.TokenManager(context);
            if (tokenManager.getAccessToken() != null) {
                request.addRequestHeader("Authorization", "Bearer " + tokenManager.getAccessToken());
            }

            DownloadManager downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
            if (downloadManager != null) {
                downloadManager.enqueue(request);
                InAppMessageUtils.show(context, context.getString(R.string.dm_download_started, safeFileName));
            }
        } catch (Exception e) {
            e.printStackTrace();
            String message = e.getMessage() != null ? e.getMessage() : context.getString(R.string.error_network_unknown);
            InAppMessageUtils.show(context, context.getString(R.string.dm_download_error, message));
        }
    }

    static class IntroHolder extends RecyclerView.ViewHolder {
        private final ItemDmProfileIntroBinding b;

        IntroHolder(ItemDmProfileIntroBinding binding) {
            super(binding.getRoot());
            this.b = binding;
        }

        void bind(@Nullable DmMessageItem intro, @Nullable String avatarUrl) {
            if (intro == null) return;
            b.tvIntroDisplayName.setText(intro.getSenderName());
            b.tvIntroUsername.setText(intro.getTimestamp());
            b.tvIntroDesc.setText(intro.getContent());
            bindAvatar(avatarUrl, intro.getSenderName());
        }

        private void bindAvatar(@Nullable String avatarUrl, @Nullable String displayName) {
            int avatarSize = b.ivIntroAvatar.getLayoutParams() != null
                    ? b.ivIntroAvatar.getLayoutParams().width
                    : b.ivIntroAvatar.getWidth();
            android.graphics.drawable.Drawable avatarFallback =
                    AvatarPlaceholderUtils.createAvatarDrawable(
                            b.ivIntroAvatar.getContext(),
                            displayName,
                            avatarSize
                    );

            String resolvedAvatarUrl = NetworkConfig.resolveUrl(avatarUrl);
            boolean hasAvatar = resolvedAvatarUrl != null && !resolvedAvatarUrl.trim().isEmpty();

            Glide.with(b.ivIntroAvatar.getContext()).clear(b.ivIntroAvatar);
            if (!hasAvatar) {
                b.ivIntroAvatar.setImageDrawable(avatarFallback);
                return;
            }

            b.ivIntroAvatar.setImageDrawable(null);
            Glide.with(b.ivIntroAvatar.getContext())
                    .load(resolvedAvatarUrl)
                    .error(avatarFallback)
                    .fallback(avatarFallback)
                    .circleCrop()
                    .into(b.ivIntroAvatar);
        }
    }

    static class ChannelWelcomeIntroHolder extends RecyclerView.ViewHolder {
        private final ItemChannelWelcomeIntroBinding b;

        ChannelWelcomeIntroHolder(ItemChannelWelcomeIntroBinding binding) {
            super(binding.getRoot());
            this.b = binding;
        }

        void bind(@Nullable DmMessageItem intro) {
            if (intro == null) return;
            b.tvWelcomeTitle.setText(intro.getSenderName());
            b.tvWelcomeSubtitle.setText(intro.getContent());
        }
    }

    static class DateSeparatorHolder extends RecyclerView.ViewHolder {
        private final ItemDmDateSeparatorBinding b;

        DateSeparatorHolder(ItemDmDateSeparatorBinding binding) {
            super(binding.getRoot());
            this.b = binding;
        }

        void bind(String label) {
            b.tvDateLabel.setText(label);
        }
    }

    /**
     * Discord-style row: avatar + name + time on the left; content flows in one column for everyone.
     */
    static class MessageRowHolder extends RecyclerView.ViewHolder {
        private final ItemDmMessageBinding b;
        @Nullable
        private final OnMessageLongClickListener onMessageLongClickListener;
        @Nullable
        private final OnReactionClickListener onReactionClickListener;

        MessageRowHolder(ItemDmMessageBinding binding,
                         @Nullable OnMessageLongClickListener onMessageLongClickListener,
                         @Nullable OnReactionClickListener onReactionClickListener) {
            super(binding.getRoot());
            this.b = binding;
            this.onMessageLongClickListener = onMessageLongClickListener;
            this.onReactionClickListener = onReactionClickListener;
        }

        void bind(DmMessageItem item, boolean showHeader, @Nullable String avatarUrl,
                  @Nullable String replyAvatarUrl,
                  @Nullable String currentUserId, boolean isLastMine, long peerLastReadAtMillis,
                  boolean showMineMessageStatus) {
            b.tvName.setText(item.getSenderName());
            b.tvTime.setText(item.getTimestamp());
            b.ivAvatar.setVisibility(showHeader ? View.VISIBLE : View.INVISIBLE);
            b.headerRow.setVisibility(showHeader ? View.VISIBLE : View.GONE);

            if (showHeader) {
                bindAvatar(avatarUrl, item.getSenderName());
            }

            int topMargin = showHeader || item.hasReply() ? dp(2) : dp(0);
            try {
                if (b.cardMessage.getLayoutParams() instanceof ConstraintLayout.LayoutParams) {
                    ConstraintLayout.LayoutParams p = (ConstraintLayout.LayoutParams) b.cardMessage.getLayoutParams();
                    p.topMargin = topMargin;
                    b.cardMessage.setLayoutParams(p);
                }
                if (b.ivMedia.getLayoutParams() instanceof ConstraintLayout.LayoutParams) {
                    ConstraintLayout.LayoutParams p = (ConstraintLayout.LayoutParams) b.ivMedia.getLayoutParams();
                    p.topMargin = topMargin;
                    b.ivMedia.setLayoutParams(p);
                }
            } catch (Exception ignored) {
            }

            if (item.hasReply()) {
                b.replyQuoteContainer.setVisibility(View.VISIBLE);
                b.tvReplyQuoteSender.setText(item.getReplyToSenderName());
                String replyContent = item.getReplyToContent();
                if (isMedia(replyContent)) {
                    String title = extractMediaTitle(replyContent);
                    b.tvReplyQuoteContent.setText(title != null ? title : b.getRoot().getContext().getString(R.string.dm_reply_media));
                } else {
                    b.tvReplyQuoteContent.setText(replyContent);
                }
                bindReplyAvatar(replyAvatarUrl, item.getReplyToSenderName());
            } else {
                b.replyQuoteContainer.setVisibility(View.GONE);
            }

            String content = item.getContent();

            if (isMedia(content)) {
                b.cardMessage.setVisibility(View.GONE);
                b.ivMedia.setVisibility(View.VISIBLE);
                String url = NetworkConfig.resolveUrl(extractMediaUrl(content));
                Glide.with(b.ivMedia.getContext())
                        .asGif()
                        .load(url)
                        .into(b.ivMedia);
            } else {
                b.cardMessage.setVisibility(View.VISIBLE);
                b.ivMedia.setVisibility(View.GONE);
                Glide.with(b.ivMedia.getContext()).clear(b.ivMedia);
                if (item.isDeleted()) {
                    b.tvMessage.setText(R.string.dm_deleted_message);
                    b.tvEdited.setVisibility(View.GONE);
                } else {
                    if (content != null && !content.isEmpty()) {
                        b.tvMessage.setVisibility(View.VISIBLE);
                        b.tvMessage.setText(content);
                    } else {
                        b.tvMessage.setVisibility(View.GONE);
                    }
                    b.tvEdited.setVisibility(item.isEdited() ? View.VISIBLE : View.GONE);
                }
            }

            loadAttachments(b.llAttachments, item.getAttachments());

            bindReactions(item, currentUserId);

            DmMessageItem.MessageStatus status = item.getStatus();
            if (item.isMine() && isLastMine && status != null && showMineMessageStatus) {
                b.tvStatus.setVisibility(View.VISIBLE);
                Context ctx = b.getRoot().getContext();
                boolean seenByPeer = peerLastReadAtMillis >= 0
                        && item.getCreatedAtMillis() >= 0
                        && item.getCreatedAtMillis() <= peerLastReadAtMillis
                        && status != DmMessageItem.MessageStatus.SENDING;
                if (seenByPeer) {
                    b.tvStatus.setText(ctx.getString(R.string.msg_status_read));
                    b.tvStatus.setAlpha(0.90f);
                    b.tvStatus.setTextColor(ctx.getColor(R.color.color_primary));
                } else {
                    switch (status) {
                        case SENDING:
                            b.tvStatus.setText(ctx.getString(R.string.msg_status_sending));
                            b.tvStatus.setAlpha(0.55f);
                            b.tvStatus.setTextColor(ctx.getColor(R.color.color_text_secondary));
                            break;
                        case SENT:
                            b.tvStatus.setText(ctx.getString(R.string.msg_status_sent));
                            b.tvStatus.setAlpha(0.65f);
                            b.tvStatus.setTextColor(ctx.getColor(R.color.color_text_secondary));
                            break;
                        case DELIVERED:
                            b.tvStatus.setText(ctx.getString(R.string.msg_status_delivered));
                            b.tvStatus.setAlpha(0.90f);
                            b.tvStatus.setTextColor(ctx.getColor(R.color.color_primary));
                            break;
                    }
                }
            } else {
                b.tvStatus.setVisibility(View.GONE);
            }

            b.getRoot().setOnLongClickListener(v -> {
                if (onMessageLongClickListener != null) {
                    onMessageLongClickListener.onMessageLongClick(item, v);
                    return true;
                }
                return false;
            });
        }

        private void bindReactions(DmMessageItem item, @Nullable String currentUserId) {
            List<ReactionDto> reactions = item.getReactions();
            b.reactionContainer.removeAllViews();
            if (reactions == null || reactions.isEmpty()) {
                b.reactionContainer.setVisibility(View.GONE);
                return;
            }
            b.reactionContainer.setVisibility(View.VISIBLE);
            Context ctx = b.getRoot().getContext();

            for (ReactionDto r : reactions) {
                boolean iReacted = currentUserId != null && r.getUserIds().contains(currentUserId);

                Chip chip = new Chip(ctx);
                chip.setText(r.getEmoji() + "  " + r.getCount());
                chip.setTextSize(13f);
                chip.setChipMinHeight(dp(28));
                chip.setChipCornerRadius(dp(14));
                chip.setChipStartPadding(dp(6));
                chip.setChipEndPadding(dp(6));
                chip.setTextStartPadding(0);
                chip.setTextEndPadding(0);
                chip.setIconStartPadding(0);
                chip.setIconEndPadding(0);
                chip.setChipIconVisible(false);
                chip.setCheckedIconVisible(false);
                chip.setEnsureMinTouchTargetSize(false);

                if (iReacted) {
                    chip.setChipBackgroundColorResource(R.color.color_primary_dark);
                    chip.setChipStrokeColorResource(R.color.color_primary);
                    chip.setChipStrokeWidth(dp(1));
                    chip.setTextColor(ctx.getColor(R.color.color_text_primary));
                } else {
                    chip.setChipBackgroundColorResource(R.color.color_surface_elevated);
                    chip.setChipStrokeColorResource(R.color.color_divider);
                    chip.setChipStrokeWidth(dp(1));
                    chip.setTextColor(ctx.getColor(R.color.color_text_secondary));
                }

                chip.setOnClickListener(v -> {
                    if (onReactionClickListener != null) {
                        onReactionClickListener.onReactionClick(item, r.getEmoji());
                    }
                });

                b.reactionContainer.addView(chip);
            }
        }

        private int dp(int value) {
            return Math.round(value * b.getRoot().getResources().getDisplayMetrics().density);
        }

        private void bindReplyAvatar(@Nullable String replyAvatarUrl, @Nullable String replyName) {
            int size = (int) (16 * b.getRoot().getContext().getResources().getDisplayMetrics().density);
            android.graphics.drawable.Drawable fallback =
                    AvatarPlaceholderUtils.createAvatarDrawable(b.ivReplyAvatar.getContext(), replyName, size);
            String resolvedUrl = NetworkConfig.resolveUrl(replyAvatarUrl);
            Glide.with(b.ivReplyAvatar.getContext()).clear(b.ivReplyAvatar);
            if (resolvedUrl == null || resolvedUrl.trim().isEmpty()) {
                b.ivReplyAvatar.setImageDrawable(fallback);
            } else {
                b.ivReplyAvatar.setImageDrawable(null);
                Glide.with(b.ivReplyAvatar.getContext())
                        .load(resolvedUrl)
                        .override(size, size)
                        .error(fallback)
                        .fallback(fallback)
                        .circleCrop()
                        .into(b.ivReplyAvatar);
            }
        }

        private void bindAvatar(@Nullable String avatarUrl, @Nullable String displayName) {
            int avatarSize = b.ivAvatar.getLayoutParams() != null
                    ? b.ivAvatar.getLayoutParams().width
                    : b.ivAvatar.getWidth();
            android.graphics.drawable.Drawable avatarFallback =
                    AvatarPlaceholderUtils.createAvatarDrawable(
                            b.ivAvatar.getContext(),
                            displayName,
                            avatarSize
                    );

            String resolvedAvatarUrl = NetworkConfig.resolveUrl(avatarUrl);
            boolean hasAvatar = resolvedAvatarUrl != null && !resolvedAvatarUrl.trim().isEmpty();

            Glide.with(b.ivAvatar.getContext()).clear(b.ivAvatar);
            if (!hasAvatar) {
                b.ivAvatar.setImageDrawable(avatarFallback);
                return;
            }

            b.ivAvatar.setImageDrawable(null);
            Glide.with(b.ivAvatar.getContext())
                    .load(resolvedAvatarUrl)
                    .error(avatarFallback)
                    .fallback(avatarFallback)
                    .circleCrop()
                    .into(b.ivAvatar);
        }
    }
}
