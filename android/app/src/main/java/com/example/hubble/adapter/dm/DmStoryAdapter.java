package com.example.hubble.adapter.dm;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.hubble.data.api.NetworkConfig;
import com.example.hubble.data.model.dm.DmConversationItem;
import com.example.hubble.databinding.ItemDmStoryBinding;
import com.example.hubble.utils.AvatarPlaceholderUtils;
import com.example.hubble.utils.UserStatusFormatter;

import java.util.ArrayList;
import java.util.List;

public class DmStoryAdapter extends RecyclerView.Adapter<DmStoryAdapter.ViewHolder> {

    public interface OnStoryClickListener {
        void onStoryClick(DmConversationItem item);
    }

    private final List<DmConversationItem> items = new ArrayList<>();
    private OnStoryClickListener listener;

    public void setItems(List<DmConversationItem> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    public void setOnStoryClickListener(OnStoryClickListener listener) {
        this.listener = listener;
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
        holder.bind(items.get(position), listener);
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

        void bind(DmConversationItem item, OnStoryClickListener listener) {
            String displayName = item.getDisplayName() != null ? item.getDisplayName().trim() : "";
            String initial = displayName.isEmpty() ? "?" : displayName.substring(0, 1).toUpperCase();
            binding.tvInitial.setText(initial);
            String customStatus = item.getCustomStatus();
            boolean hasCustomStatus = customStatus != null && !customStatus.trim().isEmpty();
            if (hasCustomStatus) {
                binding.layoutNameStatus.setVisibility(View.VISIBLE);
                binding.tvName.setText(displayName.isEmpty() ? "?" : displayName);
                binding.tvCustomStatus.setText(customStatus.trim());
                binding.tvCustomStatus.setVisibility(View.VISIBLE);
            } else {
                binding.layoutNameStatus.setVisibility(View.GONE);
            }
            bindAvatar(item);
            String status = item.getStatus();
            boolean showDot = UserStatusFormatter.isVisibleStatus(status);
            binding.viewPresence.setVisibility(showDot ? View.VISIBLE : View.GONE);
            if (showDot) {
                binding.viewPresence.setBackgroundResource(UserStatusFormatter.getStatusDotDrawable(status));
            }
            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) {
                    listener.onStoryClick(item);
                }
            });
        }

        private void bindAvatar(DmConversationItem item) {
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

            binding.tvInitial.setVisibility(View.GONE);
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
        }

        @Nullable
        private String toAbsoluteUrl(@Nullable String url) {
            return NetworkConfig.resolveUrl(url);
        }
    }
}
