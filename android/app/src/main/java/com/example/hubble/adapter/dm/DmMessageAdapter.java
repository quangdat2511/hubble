package com.example.hubble.adapter.dm;

import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.hubble.R;
import com.example.hubble.data.model.dm.AttachmentResponse;
import com.example.hubble.data.model.dm.DmMessageItem;
import com.example.hubble.databinding.ItemDmMessageMeBinding;
import com.example.hubble.databinding.ItemDmMessageOtherBinding;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DmMessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public static final String GIF_PREFIX = "{gif}";
    public static final String STICKER_PREFIX = "{sticker}";

    private static final int TYPE_ME = 1;
    private static final int TYPE_OTHER = 2;
    private static final long GROUPING_TIME_THRESHOLD_MILLIS = 7 * 60 * 1000L;

    private final List<DmMessageItem> items = new ArrayList<>();

    @Nullable
    private String currentUserAvatarUrl;
    @Nullable
    private String peerAvatarUrl;
    @Nullable
    private OnMessageLongClickListener onMessageLongClickListener;

    private static MediaPlayer currentMediaPlayer;
    private static ImageView currentPlayButton;
    private static Handler audioHandler = new Handler(Looper.getMainLooper());
    private static Runnable updateSeekBarRunnable;

    public interface OnMessageLongClickListener {
        void onMessageLongClick(@NonNull DmMessageItem item, @NonNull View anchorView);
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
        items.clear();
        if (newItems != null) {
            for (DmMessageItem item : newItems) {
                if (item != null && !item.isDeleted()) {
                    items.add(item);
                }
            }
        }
        notifyDataSetChanged();
    }

    public void appendItem(DmMessageItem item) {
        if (item == null || item.isDeleted()) return;
        items.add(item);
        notifyItemInserted(items.size() - 1);
    }

    public void upsertItem(DmMessageItem item) {
        if (item == null || item.getId() == null) return;
        for (int i = 0; i < items.size(); i++) {
            DmMessageItem old = items.get(i);
            if (item.getId().equals(old.getId())) {
                if (item.isDeleted()) {
                    items.remove(i);
                    notifyItemRemoved(i);
                } else {
                    items.set(i, item);
                    notifyItemChanged(i);
                }
                return;
            }
        }
        if (!item.isDeleted()) {
            appendItem(item);
        }
    }

    public void setOnMessageLongClickListener(@Nullable OnMessageLongClickListener listener) {
        this.onMessageLongClickListener = listener;
    }

    public void setParticipantAvatarUrls(@Nullable String currentUserAvatarUrl, @Nullable String peerAvatarUrl) {
        this.currentUserAvatarUrl = currentUserAvatarUrl;
        this.peerAvatarUrl = peerAvatarUrl;
        notifyDataSetChanged();
    }

    @Nullable
    public DmMessageItem getItem(int position) {
        if (position < 0 || position >= items.size()) return null;
        return items.get(position);
    }

    @Nullable
    public DmMessageItem getItemById(String id) {
        if (id == null) return null;
        for (DmMessageItem item : items) {
            if (id.equals(item.getId())) return item;
        }
        return null;
    }

    public void removeItemById(@Nullable String id) {
        if (id == null) return;
        for (int i = 0; i < items.size(); i++) {
            if (id.equals(items.get(i).getId())) {
                items.remove(i);
                notifyItemRemoved(i);
                return;
            }
        }
    }

    @Override
    public int getItemViewType(int position) {
        DmMessageItem item = getItem(position);
        return item != null && item.isMine() ? TYPE_ME : TYPE_OTHER;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_ME) {
            return new MeHolder(ItemDmMessageMeBinding.inflate(inflater, parent, false), onMessageLongClickListener);
        }
        return new OtherHolder(ItemDmMessageOtherBinding.inflate(inflater, parent, false), onMessageLongClickListener);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        DmMessageItem item = items.get(position);
        DmMessageItem previous = position > 0 ? items.get(position - 1) : null;
        boolean groupedWithPrevious = shouldGroupWithPrevious(item, previous);

        if (holder instanceof MeHolder) {
            ((MeHolder) holder).bind(item, !groupedWithPrevious);
        } else {
            String avatarUrl = item.isMine() ? currentUserAvatarUrl : peerAvatarUrl;
            ((OtherHolder) holder).bind(item, !groupedWithPrevious, avatarUrl);
        }
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
        return item != null && !item.isDeleted() && !item.isSystemMessage();
    }

    private static boolean safeEquals(@Nullable String a, @Nullable String b) {
        if (a == null) return b == null;
        return a.equals(b);
    }

    @Override
    public int getItemCount() {
        return items.size();
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
            String url = att.getUrl() == null ? "" : att.getUrl().replace("localhost", "10.0.2.2");

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

                mediaView.setOnClickListener(v -> openAttachment(container.getContext(), url, mimeType));
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
                String fileName = att.getFilename() != null ? att.getFilename() : "Tệp không tên";

                String safeFileName = fileName;
                if (safeFileName.contains("/")) safeFileName = safeFileName.substring(safeFileName.lastIndexOf("/") + 1);
                if (safeFileName.contains(":")) safeFileName = safeFileName.substring(safeFileName.lastIndexOf(":") + 1);
                safeFileName = safeFileName.replaceAll("[\\\\/:*?\"<>|]", "_");

                tvFileName.setText(safeFileName);

                String lowerMime = mimeType.toLowerCase();
                String lowerName = fileName.toLowerCase();

                if (lowerMime.contains("pdf") || lowerName.endsWith(".pdf")) {
                    tvFileType.setText("Tài liệu PDF");
                    ivFileIcon.setImageResource(R.drawable.ic_file_pdf);
                }
                else if (lowerMime.contains("word") || lowerMime.contains("document") || lowerName.endsWith(".docx") || lowerName.endsWith(".doc")) {
                    tvFileType.setText("Tài liệu Word");
                    ivFileIcon.setImageResource(R.drawable.ic_file_docx);
                }
                else if (lowerMime.contains("excel") || lowerMime.contains("spreadsheet") || lowerName.endsWith(".xlsx") || lowerName.endsWith(".xls")) {
                    tvFileType.setText("Bảng tính Excel");
                    ivFileIcon.setImageResource(R.drawable.ic_file_excel);
                }
                else if (lowerMime.contains("powerpoint") || lowerMime.contains("presentation") || lowerName.endsWith(".pptx") || lowerName.endsWith(".ppt")) {
                    tvFileType.setText("Bài thuyết trình");
                    ivFileIcon.setImageResource(R.drawable.ic_file_powerpoint);
                }
                else if (lowerMime.contains("zip") || lowerMime.contains("rar") || lowerName.endsWith(".zip") || lowerName.endsWith(".rar")) {
                    tvFileType.setText("Tệp nén");
                    ivFileIcon.setImageResource(R.drawable.ic_file_zip);
                }
                else if (lowerMime.startsWith("text/") || lowerName.endsWith(".txt")) {
                    tvFileType.setText("Tệp văn bản");
                    ivFileIcon.setImageResource(R.drawable.ic_file_text);
                }
                else {
                    tvFileType.setText("Tệp đính kèm");
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
            if (currentMediaPlayer != null && currentPlayButton == btnPlayPause) {
                if (currentMediaPlayer.isPlaying()) {
                    currentMediaPlayer.pause();
                    btnPlayPause.setImageResource(android.R.drawable.ic_media_play);
                } else {
                    currentMediaPlayer.start();
                    btnPlayPause.setImageResource(android.R.drawable.ic_media_pause);
                    audioHandler.post(updateSeekBarRunnable);
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
            }

            currentMediaPlayer = new MediaPlayer();
            currentPlayButton = btnPlayPause;
            currentMediaPlayer.setDataSource(url);
            currentMediaPlayer.prepareAsync();
            btnPlayPause.setImageResource(android.R.drawable.ic_popup_sync);

            currentMediaPlayer.setOnPreparedListener(mp -> {
                seekBar.setMax(mp.getDuration());
                mp.start();
                btnPlayPause.setImageResource(android.R.drawable.ic_media_pause);

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
                Toast.makeText(context, "Không thể mở tệp này", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private static void downloadFile(Context context, String url, String fileName) {
        if (url == null || url.isEmpty()) return;

        String finalUrl = url;
        if (finalUrl.contains("localhost")) {
            finalUrl = finalUrl.replace("localhost", "10.0.2.2");
        }

        String safeFileName = fileName;
        if (safeFileName.contains("/")) safeFileName = safeFileName.substring(safeFileName.lastIndexOf("/") + 1);
        if (safeFileName.contains(":")) safeFileName = safeFileName.substring(safeFileName.lastIndexOf(":") + 1);
        safeFileName = safeFileName.replaceAll("[\\\\/:*?\"<>|]", "_");

        try {
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(finalUrl));
            request.setTitle(safeFileName);
            request.setDescription("Đang tải tệp đính kèm...");
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);

            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, safeFileName);

            com.example.hubble.utils.TokenManager tokenManager = new com.example.hubble.utils.TokenManager(context);
            if (tokenManager.getAccessToken() != null) {
                request.addRequestHeader("Authorization", "Bearer " + tokenManager.getAccessToken());
            }

            DownloadManager downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
            if (downloadManager != null) {
                downloadManager.enqueue(request);
                Toast.makeText(context, "Bắt đầu tải " + safeFileName + "...", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, "Lỗi khi tải tệp: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    static class MeHolder extends RecyclerView.ViewHolder {
        private final ItemDmMessageMeBinding b;
        @Nullable
        private final OnMessageLongClickListener onMessageLongClickListener;

        MeHolder(ItemDmMessageMeBinding binding, @Nullable OnMessageLongClickListener onMessageLongClickListener) {
            super(binding.getRoot());
            this.b = binding;
            this.onMessageLongClickListener = onMessageLongClickListener;
        }

        void bind(DmMessageItem item, boolean showTimestamp) {
            b.tvTime.setText(item.getTimestamp());
            b.tvTime.setVisibility(showTimestamp ? View.VISIBLE : View.GONE);
            String content = item.getContent();

            if (item.hasReply()) {
                b.replyQuoteContainer.setVisibility(View.VISIBLE);
                b.tvReplyQuoteSender.setText(item.getReplyToSenderName());
                String replyContent = item.getReplyToContent();
                if (isMedia(replyContent)) {
                    String title = extractMediaTitle(replyContent);
                    b.tvReplyQuoteContent.setText(title != null ? title : "Media");
                } else {
                    b.tvReplyQuoteContent.setText(replyContent);
                }
            } else {
                b.replyQuoteContainer.setVisibility(View.GONE);
            }

            if (isMedia(content)) {
                b.cardMine.setVisibility(View.GONE);
                b.ivMedia.setVisibility(View.VISIBLE);
                String url = extractMediaUrl(content);
                Glide.with(b.ivMedia.getContext())
                        .asGif()
                        .load(url)
                        .into(b.ivMedia);
            } else {
                b.cardMine.setVisibility(View.VISIBLE);
                b.ivMedia.setVisibility(View.GONE);
                Glide.with(b.ivMedia.getContext()).clear(b.ivMedia);
                if (item.isDeleted()) {
                    b.tvMessage.setText("Tin nhắn đã được thu hồi");
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

            b.getRoot().setOnLongClickListener(v -> {
                if (onMessageLongClickListener != null) {
                    onMessageLongClickListener.onMessageLongClick(item, v);
                    return true;
                }
                return false;
            });
        }
    }

    static class OtherHolder extends RecyclerView.ViewHolder {
        private final ItemDmMessageOtherBinding b;
        @Nullable
        private final OnMessageLongClickListener onMessageLongClickListener;

        OtherHolder(ItemDmMessageOtherBinding binding, @Nullable OnMessageLongClickListener onMessageLongClickListener) {
            super(binding.getRoot());
            this.b = binding;
            this.onMessageLongClickListener = onMessageLongClickListener;
        }

        void bind(DmMessageItem item, boolean showHeader, @Nullable String avatarUrl) {
            b.tvName.setText(item.getSenderName());
            b.tvTime.setText(item.getTimestamp());
            b.ivAvatar.setVisibility(showHeader ? View.VISIBLE : View.INVISIBLE);

            if (b.headerRow != null) {
                b.headerRow.setVisibility(showHeader ? View.VISIBLE : View.GONE);
            } else {
                if (b.tvName != null) b.tvName.setVisibility(showHeader ? View.VISIBLE : View.GONE);
                if (b.tvTime != null) b.tvTime.setVisibility(showHeader ? View.VISIBLE : View.GONE);
            }

            if (showHeader) {
                Glide.with(b.ivAvatar.getContext())
                        .load(avatarUrl)
                        .placeholder(com.example.hubble.R.mipmap.ic_launcher_round)
                        .error(com.example.hubble.R.mipmap.ic_launcher_round)
                        .circleCrop()
                        .into(b.ivAvatar);
            }

            try {
                if (b.cardOther.getLayoutParams() instanceof ConstraintLayout.LayoutParams) {
                    ConstraintLayout.LayoutParams textParams = (ConstraintLayout.LayoutParams) b.cardOther.getLayoutParams();
                    textParams.topMargin = showHeader ? dp(2) : dp(0);
                    b.cardOther.setLayoutParams(textParams);
                }
                if (b.ivMedia.getLayoutParams() instanceof ConstraintLayout.LayoutParams) {
                    ConstraintLayout.LayoutParams mediaParams = (ConstraintLayout.LayoutParams) b.ivMedia.getLayoutParams();
                    mediaParams.topMargin = showHeader ? dp(2) : dp(0);
                    b.ivMedia.setLayoutParams(mediaParams);
                }
            } catch (Exception ignored) {}

            String content = item.getContent();

            if (item.hasReply()) {
                b.replyQuoteContainer.setVisibility(View.VISIBLE);
                b.tvReplyQuoteSender.setText(item.getReplyToSenderName());
                String replyContent = item.getReplyToContent();
                if (isMedia(replyContent)) {
                    String title = extractMediaTitle(replyContent);
                    b.tvReplyQuoteContent.setText(title != null ? title : "Media");
                } else {
                    b.tvReplyQuoteContent.setText(replyContent);
                }
            } else {
                b.replyQuoteContainer.setVisibility(View.GONE);
            }

            if (isMedia(content)) {
                b.cardOther.setVisibility(View.GONE);
                b.ivMedia.setVisibility(View.VISIBLE);
                String url = extractMediaUrl(content);
                Glide.with(b.ivMedia.getContext())
                        .asGif()
                        .load(url)
                        .into(b.ivMedia);
            } else {
                b.cardOther.setVisibility(View.VISIBLE);
                b.ivMedia.setVisibility(View.GONE);
                Glide.with(b.ivMedia.getContext()).clear(b.ivMedia);
                if (item.isDeleted()) {
                    b.tvMessage.setText("Tin nhắn đã được thu hồi");
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

            b.getRoot().setOnLongClickListener(v -> {
                if (onMessageLongClickListener != null) {
                    onMessageLongClickListener.onMessageLongClick(item, v);
                    return true;
                }
                return false;
            });
        }

        private int dp(int value) {
            return Math.round(value * b.getRoot().getResources().getDisplayMetrics().density);
        }
    }
}