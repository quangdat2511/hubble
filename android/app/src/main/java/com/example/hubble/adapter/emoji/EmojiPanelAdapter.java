package com.example.hubble.adapter.emoji;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hubble.R;
import com.example.hubble.data.model.emoji.EmojiCategory;

import java.util.ArrayList;
import java.util.List;

public class EmojiPanelAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public static final int TYPE_HEADER = 0;
    public static final int TYPE_EMOJI = 1;

    public interface OnEmojiClickListener {
        void onEmojiClick(String emoji);
    }

    private final List<Object> items = new ArrayList<>();
    private OnEmojiClickListener listener;

    public void setOnEmojiClickListener(OnEmojiClickListener l) {
        this.listener = l;
    }

    public void setCategories(List<EmojiCategory> categories) {
        items.clear();
        for (EmojiCategory cat : categories) {
            items.add(cat);
            items.addAll(cat.emojis);
        }
        notifyDataSetChanged();
    }

    public void setSearchResults(List<String> emojis) {
        items.clear();
        if (emojis != null && !emojis.isEmpty()) {
            EmojiCategory resultCat = new EmojiCategory("Results", "🔍", emojis);
            items.add(resultCat);
            items.addAll(emojis);
        }
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position) instanceof EmojiCategory ? TYPE_HEADER : TYPE_EMOJI;
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_HEADER) {
            View view = inflater.inflate(R.layout.item_emoji_section_header, parent, false);
            return new HeaderViewHolder(view);
        } else {
            View view = inflater.inflate(R.layout.item_emoji, parent, false);
            return new EmojiViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof HeaderViewHolder) {
            EmojiCategory cat = (EmojiCategory) items.get(position);
            ((HeaderViewHolder) holder).bind(cat.name);
        } else if (holder instanceof EmojiViewHolder) {
            String emoji = (String) items.get(position);
            ((EmojiViewHolder) holder).bind(emoji, listener);
        }
    }

    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvHeader;

        HeaderViewHolder(View itemView) {
            super(itemView);
            tvHeader = itemView.findViewById(R.id.tvSectionHeader);
        }

        void bind(String name) {
            tvHeader.setText(name);
        }
    }

    static class EmojiViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvEmoji;

        EmojiViewHolder(View itemView) {
            super(itemView);
            tvEmoji = itemView.findViewById(R.id.tvEmoji);
        }

        void bind(String emoji, OnEmojiClickListener listener) {
            tvEmoji.setText(emoji);
            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onEmojiClick(emoji);
            });
        }
    }
}
