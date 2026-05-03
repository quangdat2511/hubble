package com.example.hubble.adapter.search;

import android.text.SpannableString;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.hubble.R;
import com.example.hubble.data.api.NetworkConfig;
import com.example.hubble.data.model.search.SearchMessageDto;
import com.example.hubble.databinding.ItemSearchMessageBinding;
import com.example.hubble.utils.AvatarPlaceholderUtils;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SearchMessageAdapter
        extends RecyclerView.Adapter<SearchMessageAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onMessageClick(SearchMessageDto item);
    }

    private final List<SearchMessageDto> items = new ArrayList<>();
    @Nullable private OnItemClickListener listener;
    @Nullable private String highlightQuery;

    public void setItems(List<SearchMessageDto> newItems) {
        items.clear();
        if (newItems != null) items.addAll(newItems);
        notifyDataSetChanged();
    }

    public void appendItems(List<SearchMessageDto> more) {
        if (more == null || more.isEmpty()) return;
        int start = items.size();
        items.addAll(more);
        notifyItemRangeInserted(start, more.size());
    }

    public void setHighlightQuery(@Nullable String query) {
        this.highlightQuery = (query != null && !query.trim().isEmpty()) ? query.trim() : null;
    }

    public void setOnItemClickListener(@Nullable OnItemClickListener l) {
        this.listener = l;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemSearchMessageBinding binding = ItemSearchMessageBinding.inflate(
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
        private final ItemSearchMessageBinding b;

        ViewHolder(ItemSearchMessageBinding b) {
            super(b.getRoot());
            this.b = b;
        }

        void bind(SearchMessageDto item) {
            b.tvAuthorName.setText(item.getAuthorDisplayName() != null
                    ? item.getAuthorDisplayName() : item.getAuthorUsername());
            b.tvChannelName.setText(item.getChannelName() != null
                    ? "#" + item.getChannelName() : "");
            b.tvTime.setText(formatTime(item.getCreatedAt()));

            String content = item.getContent() != null ? item.getContent() : "";
            if (highlightQuery != null && !highlightQuery.isEmpty() && !content.isEmpty()) {
                b.tvContent.setText(highlight(content, highlightQuery));
            } else {
                b.tvContent.setText(com.example.hubble.view.util.MentionRenderer
                        .applyMentionSpans(b.tvContent.getContext(), content));
            }

            String avatarUrl = item.getAuthorAvatarUrl() != null
                    ? NetworkConfig.resolveUrl(item.getAuthorAvatarUrl()) : null;
            android.graphics.drawable.Drawable fallback =
                    AvatarPlaceholderUtils.createAvatarDrawable(
                            b.ivAvatar.getContext(), item.getAuthorDisplayName(), 40);
            Glide.with(b.ivAvatar.getContext())
                    .load(avatarUrl)
                    .placeholder(fallback)
                    .error(fallback)
                    .circleCrop()
                    .into(b.ivAvatar);

            b.getRoot().setOnClickListener(v -> {
                if (listener != null) listener.onMessageClick(item);
            });
        }

        private SpannableString highlight(String text, String query) {
            SpannableString spannable = new SpannableString(text);
            NormalizedText normalizedText = normalizeWithMap(text);
            String normalizedQuery = normalize(query);
            if (normalizedQuery.isEmpty() || normalizedText.normalized.isEmpty()) {
                return spannable;
            }
            int backgroundColor = b.getRoot().getContext().getResources()
                    .getColor(R.color.color_highlight, b.getRoot().getContext().getTheme());
            int foregroundColor = b.getRoot().getContext().getResources()
                    .getColor(R.color.color_highlight_text, b.getRoot().getContext().getTheme());
            int searchFrom = 0;
            while (true) {
                int normalizedStart = normalizedText.normalized.indexOf(normalizedQuery, searchFrom);
                if (normalizedStart < 0) {
                    break;
                }
                int normalizedEnd = normalizedStart + normalizedQuery.length();
                int originalStart = normalizedText.indexMap.get(normalizedStart);
                int originalEnd = normalizedText.indexMap.get(normalizedEnd - 1) + 1;
                spannable.setSpan(new BackgroundColorSpan(backgroundColor), originalStart, originalEnd,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                spannable.setSpan(new ForegroundColorSpan(foregroundColor), originalStart, originalEnd,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                searchFrom = normalizedEnd;
            }
            return spannable;
        }

        private String normalize(String value) {
            if (value == null || value.isEmpty()) {
                return "";
            }
            String lowered = value.toLowerCase(Locale.getDefault())
                    .replace('đ', 'd')
                    .replace('Đ', 'd');
            String decomposed = Normalizer.normalize(lowered, Normalizer.Form.NFD);
            return decomposed.replaceAll("\\p{M}+", "");
        }

        private NormalizedText normalizeWithMap(String original) {
            StringBuilder normalized = new StringBuilder();
            List<Integer> indexMap = new ArrayList<>();
            for (int i = 0; i < original.length(); i++) {
                String chunk = normalize(String.valueOf(original.charAt(i)));
                for (int j = 0; j < chunk.length(); j++) {
                    normalized.append(chunk.charAt(j));
                    indexMap.add(i);
                }
            }
            return new NormalizedText(normalized.toString(), indexMap);
        }

        private class NormalizedText {
            final String normalized;
            final List<Integer> indexMap;

            NormalizedText(String normalized, List<Integer> indexMap) {
                this.normalized = normalized;
                this.indexMap = indexMap;
            }
        }

        private String formatTime(String createdAt) {
            if (createdAt == null || createdAt.isEmpty()) return "";
            try {
                java.time.OffsetDateTime odt = java.time.OffsetDateTime.parse(createdAt);
                return String.format(Locale.getDefault(), "%d/%d", odt.getDayOfMonth(),
                        odt.getMonthValue());
            } catch (Exception e) {
                return "";
            }
        }
    }
}
