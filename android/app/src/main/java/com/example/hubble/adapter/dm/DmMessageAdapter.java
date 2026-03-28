package com.example.hubble.adapter.dm;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.hubble.data.model.dm.DmMessageItem;
import com.example.hubble.databinding.ItemDmMessageMeBinding;
import com.example.hubble.databinding.ItemDmMessageOtherBinding;

import java.util.ArrayList;
import java.util.List;

public class DmMessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    // Message type prefix constants — also used in DmChatActivity when sending
    public static final String GIF_PREFIX = "{gif}";
    public static final String STICKER_PREFIX = "{sticker}";

    private static final int TYPE_ME = 1;
    private static final int TYPE_OTHER = 2;

    private final List<DmMessageItem> items = new ArrayList<>();
    @Nullable
    private OnMessageLongClickListener onMessageLongClickListener;

    public interface OnMessageLongClickListener {
        void onMessageLongClick(@NonNull DmMessageItem item, @NonNull View anchorView);
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    public static boolean isGif(String content) {
        return content != null && content.startsWith(GIF_PREFIX);
    }

    public static boolean isSticker(String content) {
        return content != null && content.startsWith(STICKER_PREFIX);
    }

    public static boolean isMedia(String content) {
        return isGif(content) || isSticker(content);
    }

    /**
     * Extracts the URL from a media message.
     * Handles both old format "{gif}url" and new format "{gif}title\nurl".
     */
    public static String extractMediaUrl(String content) {
        String body = null;
        if (isGif(content)) body = content.substring(GIF_PREFIX.length());
        else if (isSticker(content)) body = content.substring(STICKER_PREFIX.length());
        else return content;

        int nl = body.indexOf('\n');
        return nl >= 0 ? body.substring(nl + 1) : body;
    }

    /**
     * Extracts the human-readable title from a media message, or null if none is stored.
     * Handles both old format "{gif}url" and new format "{gif}title\nurl".
     */
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

    public void upsertItem(DmMessageItem item) {
        if (item == null || item.getId() == null) return;
        for (int i = 0; i < items.size(); i++) {
            DmMessageItem old = items.get(i);
            if (item.getId().equals(old.getId())) {
                items.set(i, item);
                notifyItemChanged(i);
                return;
            }
        }
        appendItem(item);
    }

    public void setOnMessageLongClickListener(@Nullable OnMessageLongClickListener listener) {
        this.onMessageLongClickListener = listener;
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

    @Override
    public int getItemViewType(int position) {
        return items.get(position).isMine() ? TYPE_ME : TYPE_OTHER;
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
        if (holder instanceof MeHolder) {
            ((MeHolder) holder).bind(item);
        } else {
            ((OtherHolder) holder).bind(item);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    // ── ViewHolders ────────────────────────────────────────────────────────

    static class MeHolder extends RecyclerView.ViewHolder {
        private final ItemDmMessageMeBinding b;
        @Nullable
        private final OnMessageLongClickListener onMessageLongClickListener;

        MeHolder(ItemDmMessageMeBinding binding, @Nullable OnMessageLongClickListener onMessageLongClickListener) {
            super(binding.getRoot());
            this.b = binding;
            this.onMessageLongClickListener = onMessageLongClickListener;
        }

        void bind(DmMessageItem item) {
            b.tvTime.setText(item.getTimestamp());
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
                // Hide text bubble, show image
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
                String display = content;
                if (item.isDeleted()) {
                    display = "Tin nhắn đã được thu hồi";
                } else if (item.isEdited()) {
                    display = content + " (đã chỉnh sửa)";
                }
                b.tvMessage.setText(display);
            }

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

        void bind(DmMessageItem item) {
            b.tvName.setText(item.getSenderName());
            b.tvTime.setText(item.getTimestamp());
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
                // Hide text bubble, show image
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
                String display = content;
                if (item.isDeleted()) {
                    display = "Tin nhắn đã được thu hồi";
                } else if (item.isEdited()) {
                    display = content + " (đã chỉnh sửa)";
                }
                b.tvMessage.setText(display);
            }

            b.getRoot().setOnLongClickListener(v -> {
                if (onMessageLongClickListener != null) {
                    onMessageLongClickListener.onMessageLongClick(item, v);
                    return true;
                }
                return false;
            });
        }
    }
}
