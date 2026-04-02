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
import com.example.hubble.databinding.ItemDmStoryBinding;
import com.example.hubble.utils.AvatarPlaceholderUtils;

import java.util.ArrayList;
import java.util.List;

public class DmStoryAdapter extends RecyclerView.Adapter<DmStoryAdapter.ViewHolder> {

    private final List<DmConversationItem> items = new ArrayList<>();

    public void setItems(List<DmConversationItem> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemDmStoryBinding binding = ItemDmStoryBinding.inflate(
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

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemDmStoryBinding binding;

        ViewHolder(ItemDmStoryBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(DmConversationItem item) {
            int avatarSize = binding.ivAvatar.getLayoutParams() != null
                    ? binding.ivAvatar.getLayoutParams().width
                    : binding.ivAvatar.getWidth();
            android.graphics.drawable.Drawable avatarFallback =
                    AvatarPlaceholderUtils.createAvatarDrawable(
                            binding.ivAvatar.getContext(),
                            item.getDisplayName(),
                            avatarSize
                    );
            binding.tvInitial.setVisibility(View.GONE);
            Glide.with(binding.ivAvatar.getContext())
                    .load(toAbsoluteUrl(item.getAvatarUrl()))
                    .placeholder(avatarFallback)
                    .error(avatarFallback)
                    .circleCrop()
                    .into(binding.ivAvatar);
            binding.viewPresence.setVisibility(item.isOnline() ? View.VISIBLE : View.GONE);
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
}


