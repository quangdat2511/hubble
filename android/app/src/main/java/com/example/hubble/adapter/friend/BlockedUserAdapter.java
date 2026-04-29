package com.example.hubble.adapter.friend;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.hubble.data.api.NetworkConfig;
import com.example.hubble.data.model.dm.FriendUserDto;
import com.example.hubble.databinding.ItemBlockedUserBinding;
import com.example.hubble.utils.AvatarPlaceholderUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for the Blocked Users screen.
 * Each item shows avatar, name, and an "Unblock" button.
 */
public class BlockedUserAdapter extends RecyclerView.Adapter<BlockedUserAdapter.ViewHolder> {

    private final List<FriendUserDto> users = new ArrayList<>();
    private final OnUnblockClickListener listener;

    public interface OnUnblockClickListener {
        void onUnblockClick(FriendUserDto user);
    }

    public BlockedUserAdapter(OnUnblockClickListener listener) {
        this.listener = listener;
    }

    public void setUsers(List<FriendUserDto> newUsers) {
        users.clear();
        if (newUsers != null) {
            users.addAll(newUsers);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemBlockedUserBinding binding = ItemBlockedUserBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(users.get(position));
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemBlockedUserBinding binding;

        ViewHolder(ItemBlockedUserBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(FriendUserDto user) {
            String displayName = (user.getDisplayName() != null && !user.getDisplayName().isEmpty())
                    ? user.getDisplayName() : user.getUsername();
            binding.tvDisplayName.setText(displayName);
            binding.tvUsername.setText("@" + user.getUsername());
            bindAvatar(user, displayName);

            binding.btnUnblock.setOnClickListener(v -> {
                if (listener != null) listener.onUnblockClick(user);
            });
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
