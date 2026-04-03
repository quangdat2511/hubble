package com.example.hubble.adapter.dm;

import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hubble.data.model.dm.DmConversationItem;
import com.example.hubble.databinding.ItemDmConversationBinding;
import com.example.hubble.databinding.ItemDmSectionHeaderBinding;
import com.google.android.material.color.MaterialColors;

import static com.example.hubble.adapter.dm.DmMessageAdapter.GIF_PREFIX;
import static com.example.hubble.adapter.dm.DmMessageAdapter.STICKER_PREFIX;

import java.util.ArrayList;
import java.util.List;

public class DmConversationAdapter extends RecyclerView.Adapter<DmConversationAdapter.ViewHolder> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_CONVERSATION = 1;

    public interface OnConversationClickListener {
        void onConversationClick(DmConversationItem item);
    }

    public interface OnConversationLongClickListener {
        void onConversationLongClick(DmConversationItem item);
    }

    private final List<DmConversationItem> items = new ArrayList<>();
    private final List<RowItem> rows = new ArrayList<>();
    private OnConversationClickListener listener;
    private OnConversationLongClickListener longClickListener;

    public void setItems(List<DmConversationItem> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        rebuildRows();
        notifyDataSetChanged();
    }

    public void setOnConversationClickListener(OnConversationClickListener listener) {
        this.listener = listener;
    }

    public void setOnConversationLongClickListener(OnConversationLongClickListener longClickListener) {
        this.longClickListener = longClickListener;
    }

    private void rebuildRows() {
        rows.clear();
        if (items.isEmpty()) {
            return;
        }

        List<DmConversationItem> favoriteItems = new ArrayList<>();
        List<DmConversationItem> regularItems = new ArrayList<>();

        for (DmConversationItem item : items) {
            if (item != null && item.isFavorite()) {
                favoriteItems.add(item);
            } else if (item != null) {
                regularItems.add(item);
            }
        }

        if (!favoriteItems.isEmpty()) {
            rows.add(RowItem.header("Favorites"));
            for (DmConversationItem item : favoriteItems) {
                rows.add(RowItem.conversation(item));
            }
        }

        if (!regularItems.isEmpty()) {
            rows.add(RowItem.header("Messages"));
            for (DmConversationItem item : regularItems) {
                rows.add(RowItem.conversation(item));
            }
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (position < 0 || position >= rows.size()) {
            return TYPE_CONVERSATION;
        }
        return rows.get(position).isHeader ? TYPE_HEADER : TYPE_CONVERSATION;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_HEADER) {
            ItemDmSectionHeaderBinding headerBinding = ItemDmSectionHeaderBinding.inflate(
                    LayoutInflater.from(parent.getContext()), parent, false);
            return new HeaderViewHolder(headerBinding);
        }

        ItemDmConversationBinding binding = ItemDmConversationBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ConversationViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RowItem row = rows.get(position);
        if (holder instanceof HeaderViewHolder) {
            ((HeaderViewHolder) holder).bind(row.headerTitle);
            return;
        }
        if (holder instanceof ConversationViewHolder) {
            ((ConversationViewHolder) holder).bind(row.conversation);
        }
    }

    @Override
    public int getItemCount() {
        return rows.size();
    }

    abstract static class ViewHolder extends RecyclerView.ViewHolder {
        ViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }

    class HeaderViewHolder extends ViewHolder {
        private final ItemDmSectionHeaderBinding binding;

        HeaderViewHolder(ItemDmSectionHeaderBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(String title) {
            binding.tvSectionTitle.setText(title);
        }
    }

    class ConversationViewHolder extends ViewHolder {
        private final ItemDmConversationBinding binding;

        ConversationViewHolder(ItemDmConversationBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(DmConversationItem item) {
            binding.tvName.setText(item.getDisplayName());
            binding.tvPreview.setText(formatPreview(item.getLastMessage()));
            binding.tvTime.setText(item.getTimeLabel());
            binding.viewPresence.setVisibility(item.isOnline() ? View.VISIBLE : View.GONE);
            binding.chipOfficial.setVisibility(item.isVerified() ? View.VISIBLE : View.GONE);
            int cardColor = item.isSelected()
                    ? MaterialColors.getColor(binding.getRoot(), com.google.android.material.R.attr.colorSurfaceContainerHighest)
                    : MaterialColors.getColor(binding.getRoot(), com.google.android.material.R.attr.colorSurface);
            int strokeColor = MaterialColors.getColor(binding.getRoot(), com.google.android.material.R.attr.colorOutline);
            binding.containerRow.setCardBackgroundColor(cardColor);
            binding.containerRow.setStrokeColor(strokeColor);
            int onSurface = MaterialColors.getColor(binding.getRoot(), com.google.android.material.R.attr.colorOnSurface);
            int onSurfaceVariant = MaterialColors.getColor(binding.getRoot(), com.google.android.material.R.attr.colorOnSurfaceVariant);

            boolean unread = item.hasUnread();
            binding.viewUnreadDot.setVisibility(unread ? View.VISIBLE : View.GONE);

            if (item.isSelected()) {
                binding.tvName.setTypeface(null, unread ? Typeface.BOLD : Typeface.NORMAL);
                binding.tvPreview.setTypeface(null, unread ? Typeface.BOLD : Typeface.NORMAL);
                binding.tvName.setTextColor(onSurface);
                binding.tvPreview.setTextColor(unread ? onSurface : onSurfaceVariant);
            } else if (unread) {
                binding.tvName.setTypeface(null, Typeface.BOLD);
                binding.tvPreview.setTypeface(null, Typeface.BOLD);
                binding.tvName.setTextColor(onSurface);
                binding.tvPreview.setTextColor(onSurface);
            } else {
                binding.tvName.setTypeface(null, Typeface.NORMAL);
                binding.tvPreview.setTypeface(null, Typeface.NORMAL);
                binding.tvName.setTextColor(onSurfaceVariant);
                binding.tvPreview.setTextColor(onSurfaceVariant);
            }
            binding.ivFavorite.setVisibility(item.isFavorite() ? View.VISIBLE : View.GONE);

            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) {
                    listener.onConversationClick(item);
                }
            });

            binding.getRoot().setOnLongClickListener(v -> {
                if (longClickListener != null) {
                    longClickListener.onConversationLongClick(item);
                    return true;
                }
                return false;
            });
        }

        /**
         * Replaces raw {gif}/{sticker} prefixed content with a Discord-style human-readable label,
         * preserving any "SenderName: " prefix that may precede the content.
         *
         * Supports both legacy format "{gif}url" and new format "{gif}title\nurl".
         *
         * Examples:
         *   "You: {gif}Cheer\nhttps://..."     → "You: Cheer 🎬"
         *   "QDat: {gif}https://..."            → "QDat: GIF 🎬"
         *   "You: {sticker}Hype\nhttps://..."  → "You: Hype 🎭"
         *   "Hello world"                       → "Hello world"  (unchanged)
         */
        private String formatPreview(String raw) {
            if (raw == null) return "";

            String prefix = null;
            String mediaContent = null;
            boolean isGif = false;

            if (raw.contains(GIF_PREFIX)) {
                int idx = raw.indexOf(GIF_PREFIX);
                prefix = raw.substring(0, idx);
                mediaContent = raw.substring(idx + GIF_PREFIX.length());
                isGif = true;
            } else if (raw.contains(STICKER_PREFIX)) {
                int idx = raw.indexOf(STICKER_PREFIX);
                prefix = raw.substring(0, idx);
                mediaContent = raw.substring(idx + STICKER_PREFIX.length());
                isGif = false;
            }

            if (mediaContent != null) {
                String icon = isGif ? "🎬" : "🎭";
                String defaultLabel = isGif ? "GIF" : "Sticker";
                int nl = mediaContent.indexOf('\n');
                String title = nl > 0 ? mediaContent.substring(0, nl).trim() : null;
                String label = (title != null && !title.isEmpty()) ? title : defaultLabel;
                return prefix + label + " " + icon;
            }

            return raw;
        }
    }

    static class RowItem {
        final boolean isHeader;
        final String headerTitle;
        final DmConversationItem conversation;

        private RowItem(boolean isHeader, @Nullable String headerTitle, @Nullable DmConversationItem conversation) {
            this.isHeader = isHeader;
            this.headerTitle = headerTitle;
            this.conversation = conversation;
        }

        static RowItem header(String title) {
            return new RowItem(true, title, null);
        }

        static RowItem conversation(DmConversationItem item) {
            return new RowItem(false, null, item);
        }
    }
}


