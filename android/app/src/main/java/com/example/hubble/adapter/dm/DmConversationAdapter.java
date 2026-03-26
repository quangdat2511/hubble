package com.example.hubble.adapter.dm;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hubble.data.model.dm.DmConversationItem;
import com.example.hubble.databinding.ItemDmConversationBinding;
import com.google.android.material.color.MaterialColors;

import static com.example.hubble.adapter.dm.DmMessageAdapter.GIF_PREFIX;
import static com.example.hubble.adapter.dm.DmMessageAdapter.STICKER_PREFIX;

import java.util.ArrayList;
import java.util.List;

public class DmConversationAdapter extends RecyclerView.Adapter<DmConversationAdapter.ViewHolder> {

    public interface OnConversationClickListener {
        void onConversationClick(DmConversationItem item);
    }

    private final List<DmConversationItem> items = new ArrayList<>();
    private OnConversationClickListener listener;

    public void setItems(List<DmConversationItem> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    public void setOnConversationClickListener(OnConversationClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemDmConversationBinding binding = ItemDmConversationBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemDmConversationBinding binding;

        ViewHolder(ItemDmConversationBinding binding) {
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

            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) {
                    listener.onConversationClick(item);
                }
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
}


