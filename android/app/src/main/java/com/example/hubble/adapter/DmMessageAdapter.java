package com.example.hubble.adapter;

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
import com.example.hubble.data.model.AttachmentResponse;
import com.example.hubble.data.model.DmMessageItem;
import com.example.hubble.databinding.ItemDmMessageMeBinding;
import com.example.hubble.databinding.ItemDmMessageOtherBinding;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DmMessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_ME = 1;
    private static final int TYPE_OTHER = 2;

    private final List<DmMessageItem> items = new ArrayList<>();

    private static MediaPlayer currentMediaPlayer;
    private static ImageView currentPlayButton;
    private static Handler audioHandler = new Handler(Looper.getMainLooper());
    private static Runnable updateSeekBarRunnable;

    public void setItems(List<DmMessageItem> newItems) {
        items.clear();
        if (newItems != null) items.addAll(newItems);
        notifyDataSetChanged();
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

    // ── Shared utility ────────────────────────────────────────────────────────

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
                // 1. Inflate Layout cho Media
                View mediaView = inflater.inflate(R.layout.item_attachment_media, container, false);
                ImageView ivMedia = mediaView.findViewById(R.id.ivMedia);
                ImageView ivPlayIcon = mediaView.findViewById(R.id.ivPlayIcon);

                // Hiện nút Play nếu là video
                ivPlayIcon.setVisibility(mimeType.startsWith("video/") ? View.VISIBLE : View.GONE);

                Glide.with(container.getContext())
                        .load(url)
                        .centerCrop()
                        .placeholder(android.R.drawable.ic_menu_gallery)
                        .error(android.R.drawable.ic_menu_report_image)
                        .into(ivMedia);

                mediaView.setOnClickListener(v -> openAttachment(container.getContext(), url, mimeType));

                container.addView(mediaView);

            }
            else if (mimeType.startsWith("audio/") || mimeType.endsWith("m4a") || mimeType.contains("mp4")) {
                // INFLATE LAYOUT VOICE
                View voiceView = LayoutInflater.from(container.getContext())
                        .inflate(R.layout.item_attachment_voice, container, false);

                ImageView btnPlayPause = voiceView.findViewById(R.id.btnPlayPause);
                SeekBar seekBarVoice = voiceView.findViewById(R.id.seekBarVoice);
                TextView tvDuration = voiceView.findViewById(R.id.tvDuration);

                btnPlayPause.setOnClickListener(v -> playAudio(url, btnPlayPause, seekBarVoice, tvDuration));

                container.addView(voiceView);
            }
            else {
                // 2. Inflate Layout cho File tài liệu
                View fileView = inflater.inflate(R.layout.item_attachment_file, container, false);
                TextView tvFileName = fileView.findViewById(R.id.tvFileName);
                TextView tvFileType = fileView.findViewById(R.id.tvFileType);

                String fileName = att.getFilename() != null ? att.getFilename() : "Tệp không tên";
                tvFileName.setText(fileName);

                // Gán loại tệp cơ bản để hiển thị cho đẹp
                if (mimeType.contains("pdf")) tvFileType.setText("Tài liệu PDF");
                else if (mimeType.contains("zip") || mimeType.contains("rar")) tvFileType.setText("Tệp nén");
                else tvFileType.setText("Tệp đính kèm");

                fileView.setOnClickListener(v -> openAttachment(container.getContext(), url, mimeType));

                container.addView(fileView);
            }
        }

        container.setVisibility(View.VISIBLE);
    }

    // ── MeHolder ──────────────────────────────────────────────────────────────

    static class MeHolder extends RecyclerView.ViewHolder {
        private final ItemDmMessageMeBinding binding;

        MeHolder(ItemDmMessageMeBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(DmMessageItem item) {
            binding.tvTime.setText(item.getTimestamp());

            if (item.getContent() != null && !item.getContent().isEmpty()) {
                binding.tvMessage.setVisibility(View.VISIBLE);
                binding.tvMessage.setText(item.getContent());
            } else {
                binding.tvMessage.setVisibility(View.GONE);
            }

            loadAttachments(binding.llAttachments, item.getAttachments());
        }
    }

    // ── OtherHolder ───────────────────────────────────────────────────────────

    static class OtherHolder extends RecyclerView.ViewHolder {
        private final ItemDmMessageOtherBinding binding;

        OtherHolder(ItemDmMessageOtherBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(DmMessageItem item) {
            binding.tvName.setText(item.getSenderName());
            binding.tvTime.setText(item.getTimestamp());

            if (item.getContent() != null && !item.getContent().isEmpty()) {
                binding.tvMessage.setVisibility(View.VISIBLE);
                binding.tvMessage.setText(item.getContent());
            } else {
                binding.tvMessage.setVisibility(View.GONE);
            }

            loadAttachments(binding.llAttachments, item.getAttachments());
        }
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
            btnPlayPause.setImageResource(android.R.drawable.ic_popup_sync); // Icon xoay xoay đang load

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
            // Gắn URL và báo cho Android biết đây là loại file gì để nó tìm app phù hợp
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