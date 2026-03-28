package com.example.hubble.adapter.dm;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
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

    // Prefix từ nhánh main
    public static final String GIF_PREFIX = "{gif}";
    public static final String STICKER_PREFIX = "{sticker}";

    private static final int TYPE_ME = 1;
    private static final int TYPE_OTHER = 2;

    private final List<DmMessageItem> items = new ArrayList<>();

    // Biến cho Voice Message từ nhánh media
    private static MediaPlayer currentMediaPlayer;
    private static ImageView currentPlayButton;
    private static Handler audioHandler = new Handler(Looper.getMainLooper());
    private static Runnable updateSeekBarRunnable;

    // ── Helpers (Main) ──────────────────────────────────────────────────────

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

    // ── Adapter interface ──────────────────────────────────────────────────

    public void setItems(List<DmMessageItem> newItems) {
        items.clear();
        if (newItems != null) items.addAll(newItems);
        notifyDataSetChanged();
    }

    public void appendItem(DmMessageItem item) {
        if (item == null) return;
        items.add(item);
        notifyItemInserted(items.size() - 1);
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position).isMine() ? TYPE_ME : TYPE_OTHER;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_ME) {
            return new MeHolder(ItemDmMessageMeBinding.inflate(inflater, parent, false));
        }
        return new OtherHolder(ItemDmMessageOtherBinding.inflate(inflater, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        DmMessageItem item = items.get(position);
        if (holder instanceof MeHolder) ((MeHolder) holder).bind(item);
        else ((OtherHolder) holder).bind(item);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    // ── Attachments Utility (Media) ─────────────────────────────────────────

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

                String fileName = att.getFilename() != null ? att.getFilename() : "Tệp không tên";
                tvFileName.setText(fileName);

                String lowerMime = mimeType.toLowerCase();
                String lowerName = fileName.toLowerCase();

                if (lowerMime.contains("pdf") || lowerName.endsWith(".pdf")) {
                    tvFileType.setText("Tài liệu PDF");
                    ivFileIcon.setImageResource(R.drawable.ic_file_pdf);
                }
                else if (lowerMime.contains("word") || lowerName.endsWith(".docx") || lowerName.endsWith(".doc")) {
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

                fileView.setOnClickListener(v -> openAttachment(container.getContext(), url, mimeType));
                container.addView(fileView);
            }
        }
        container.setVisibility(View.VISIBLE);
    }

    // ── ViewHolders (Gộp Main + Media) ──────────────────────────────────────

    static class MeHolder extends RecyclerView.ViewHolder {
        private final ItemDmMessageMeBinding b;

        MeHolder(ItemDmMessageMeBinding binding) {
            super(binding.getRoot());
            this.b = binding;
        }

        void bind(DmMessageItem item) {
            b.tvTime.setText(item.getTimestamp());
            String content = item.getContent();

            // Xử lý Text/GIF từ Main
            if (isMedia(content)) {
                b.cardMine.setVisibility(View.GONE);
                b.ivMedia.setVisibility(View.VISIBLE);
                String url = extractMediaUrl(content);
                Glide.with(b.ivMedia.getContext()).asGif().load(url).into(b.ivMedia);
            } else {
                b.cardMine.setVisibility(View.VISIBLE);
                b.ivMedia.setVisibility(View.GONE);
                Glide.with(b.ivMedia.getContext()).clear(b.ivMedia);

                if (content != null && !content.isEmpty()) {
                    b.tvMessage.setVisibility(View.VISIBLE);
                    b.tvMessage.setText(content);
                } else {
                    b.tvMessage.setVisibility(View.GONE);
                }
            }

            // Xử lý đính kèm File/Voice từ Media
            loadAttachments(b.llAttachments, item.getAttachments());
        }
    }

    static class OtherHolder extends RecyclerView.ViewHolder {
        private final ItemDmMessageOtherBinding b;

        OtherHolder(ItemDmMessageOtherBinding binding) {
            super(binding.getRoot());
            this.b = binding;
        }

        void bind(DmMessageItem item) {
            b.tvName.setText(item.getSenderName());
            b.tvTime.setText(item.getTimestamp());
            String content = item.getContent();

            if (isMedia(content)) {
                b.cardOther.setVisibility(View.GONE);
                b.ivMedia.setVisibility(View.VISIBLE);
                String url = extractMediaUrl(content);
                Glide.with(b.ivMedia.getContext()).asGif().load(url).into(b.ivMedia);
            } else {
                b.cardOther.setVisibility(View.VISIBLE);
                b.ivMedia.setVisibility(View.GONE);
                Glide.with(b.ivMedia.getContext()).clear(b.ivMedia);

                if (content != null && !content.isEmpty()) {
                    b.tvMessage.setVisibility(View.VISIBLE);
                    b.tvMessage.setText(content);
                } else {
                    b.tvMessage.setVisibility(View.GONE);
                }
            }

            loadAttachments(b.llAttachments, item.getAttachments());
        }
    }

    // ── Audio & File Open Utilities ─────────────────────────────────────────

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
}