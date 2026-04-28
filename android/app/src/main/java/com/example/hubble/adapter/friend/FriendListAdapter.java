package com.example.hubble.adapter.friend;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.hubble.data.api.NetworkConfig;
import com.example.hubble.data.model.dm.FriendUserDto;
import com.example.hubble.databinding.ItemFriendBinding;
import com.example.hubble.utils.AvatarPlaceholderUtils;

import java.util.ArrayList;
import java.util.List;

public class FriendListAdapter extends RecyclerView.Adapter<FriendListAdapter.ViewHolder> {

    private final List<FriendUserDto> friends = new ArrayList<>();
    private final OnUnfriendClickListener listener;

    public interface OnUnfriendClickListener {
        void onUnfriendClick(FriendUserDto user);
    }

    public FriendListAdapter(OnUnfriendClickListener listener) {
        this.listener = listener;
    }

    public void setFriends(List<FriendUserDto> newFriends) {
        friends.clear();
        if (newFriends != null) friends.addAll(newFriends);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemFriendBinding binding = ItemFriendBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(friends.get(position));
    }

    @Override
    public int getItemCount() {
        return friends.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemFriendBinding binding;

        ViewHolder(ItemFriendBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(FriendUserDto user) {
            String displayName = (user.getDisplayName() != null && !user.getDisplayName().isEmpty())
                    ? user.getDisplayName() : user.getUsername();
            binding.tvDisplayName.setText(displayName);
            binding.tvUsername.setText("@" + user.getUsername());

            binding.btnUnfriend.setOnClickListener(v -> {
                if (listener != null) listener.onUnfriendClick(user);
            });

            bindAvatar(user, displayName);
        }

        private void bindAvatar(FriendUserDto user, String displayName) {
            int avatarSize = binding.ivAvatar.getLayoutParams() != null
                    ? binding.ivAvatar.getLayoutParams().width
                    : binding.ivAvatar.getWidth();
            android.graphics.drawable.Drawable avatarFallback =
                    AvatarPlaceholderUtils.createAvatarDrawable(
                            binding.ivAvatar.getContext(),
                            displayName,
                            avatarSize
                    );

            Glide.with(binding.ivAvatar.getContext())
                    .load(toAbsoluteUrl(user.getAvatarUrl()))
                    .placeholder(avatarFallback)
                    .error(avatarFallback)
                    .fallback(avatarFallback)
                    .circleCrop()
                    .into(binding.ivAvatar);
        }

        @Nullable
        private String toAbsoluteUrl(@Nullable String url) {
            return NetworkConfig.resolveUrl(url);
        }
    }
}
