package com.example.hubble.adapter.dm;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.hubble.data.api.RetrofitClient;
import com.example.hubble.data.model.dm.DmConversationItem;
import com.example.hubble.databinding.ItemDmConversationBinding;
import com.example.hubble.databinding.ItemDmSectionHeaderBinding;
import com.example.hubble.utils.AvatarPlaceholderUtils;
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
            binding.tvName.setTextColor(item.isSelected()
                    ? MaterialColors.getColor(binding.getRoot(), com.google.android.material.R.attr.colorOnSurface)
                    : MaterialColors.getColor(binding.getRoot(), com.google.android.material.R.attr.colorOnSurfaceVariant));
            binding.ivFavorite.setVisibility(item.isFavorite() ? View.VISIBLE : View.GONE);
            int avatarSize = binding.ivAvatar.getLayoutParams() != null
                    ? binding.ivAvatar.getLayoutParams().width
                    : binding.ivAvatar.getWidth();
            android.graphics.drawable.Drawable avatarFallback =
                    AvatarPlaceholderUtils.createAvatarDrawable(
                            binding.ivAvatar.getContext(),
                            item.getDisplayName(),
                            avatarSize
                    );
            String avatarUrl = toAbsoluteUrl(item.getAvatarUrl());
            boolean hasAvatar = avatarUrl != null && !avatarUrl.trim().isEmpty();

            Glide.with(binding.ivAvatar.getContext()).clear(binding.ivAvatar);
            if (!hasAvatar) {
                binding.ivAvatar.setImageDrawable(avatarFallback);
            } else {
                binding.ivAvatar.setImageDrawable(null);
                Glide.with(binding.ivAvatar.getContext())
                        .load(avatarUrl)
                        .error(avatarFallback)
                        .fallback(avatarFallback)
                        .circleCrop()
                        .into(binding.ivAvatar);
            }

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

        @Nullable
        private String toAbsoluteUrl(@Nullable String url) {
            if (url == null || url.trim().isEmpty()) {
                return null;
            }

            String trimmedUrl = url.trim();
            if (trimmedUrl.startsWith("http://") || trimmedUrl.startsWith("https://")) {
                return trimmedUrl.replace("localhost", "10.0.2.2");
            }

            String baseUrl = RetrofitClient.getBaseUrl();
            if (baseUrl.endsWith("/") && trimmedUrl.startsWith("/")) {
                return baseUrl.substring(0, baseUrl.length() - 1) + trimmedUrl;
            }
            if (!baseUrl.endsWith("/") && !trimmedUrl.startsWith("/")) {
                return baseUrl + "/" + trimmedUrl;
            }
            return baseUrl + trimmedUrl;
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

