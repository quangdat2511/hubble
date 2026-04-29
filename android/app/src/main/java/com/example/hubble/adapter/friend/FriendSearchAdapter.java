package com.example.hubble.adapter.friend;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.hubble.data.api.NetworkConfig;
import com.example.hubble.data.model.dm.FriendUserDto;
import com.example.hubble.databinding.ItemFriendSearchBinding;
import com.example.hubble.utils.AvatarPlaceholderUtils;

import java.util.ArrayList;
import java.util.List;

public class FriendSearchAdapter extends RecyclerView.Adapter<FriendSearchAdapter.ViewHolder> {
    private final List<FriendUserDto> users = new ArrayList<>();
    private final OnAddClickListener addListener;
    private final OnBlockClickListener blockListener;

    public interface OnAddClickListener {
        void onAddClick(FriendUserDto user);
    }

    public interface OnBlockClickListener {
        void onBlockClick(FriendUserDto user);
    }

    /** Constructor with only add listener (backward-compat). Block button will be hidden. */
    public FriendSearchAdapter(OnAddClickListener addListener) {
        this.addListener = addListener;
        this.blockListener = null;
    }

    /** Constructor with both add and block listeners. */
    public FriendSearchAdapter(OnAddClickListener addListener, OnBlockClickListener blockListener) {
        this.addListener = addListener;
        this.blockListener = blockListener;
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
        ItemFriendSearchBinding binding = ItemFriendSearchBinding.inflate(
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
        private final ItemFriendSearchBinding binding;

        ViewHolder(ItemFriendSearchBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(FriendUserDto user) {
            String displayName = (user.getDisplayName() != null && !user.getDisplayName().isEmpty())
                    ? user.getDisplayName() : user.getUsername();
            binding.tvDisplayName.setText(displayName);
            binding.tvUsername.setText("@" + user.getUsername());
            bindAvatar(user, displayName);
            bindActions(user);
        }

        private void bindActions(FriendUserDto user) {
            String status = user.getRelationStatus();
            if (status == null) status = "NONE";

            // Reset all first
            binding.btnAdd.setVisibility(View.GONE);
            binding.btnBlock.setVisibility(View.GONE);
            binding.tvStatus.setVisibility(View.GONE);

            switch (status.toUpperCase()) {
                case "NONE":
                    // Can add friend + can block
                    binding.btnAdd.setVisibility(View.VISIBLE);
                    binding.btnAdd.setOnClickListener(v -> {
                        if (addListener != null) addListener.onAddClick(user);
                    });
                    if (blockListener != null) {
                        binding.btnBlock.setVisibility(View.VISIBLE);
                        binding.btnBlock.setOnClickListener(v -> blockListener.onBlockClick(user));
                    }
                    break;

                case "FRIEND":
                    // Already friends: no add, but can block
                    binding.tvStatus.setVisibility(View.VISIBLE);
                    binding.tvStatus.setText("Bạn bè");
                    if (blockListener != null) {
                        binding.btnBlock.setVisibility(View.VISIBLE);
                        binding.btnBlock.setOnClickListener(v -> blockListener.onBlockClick(user));
                    }
                    break;

                case "PENDING_OUTGOING":
                    binding.tvStatus.setVisibility(View.VISIBLE);
                    binding.tvStatus.setText("Đã gửi lời mời");
                    break;

                case "PENDING_INCOMING":
                    binding.tvStatus.setVisibility(View.VISIBLE);
                    binding.tvStatus.setText("Chờ chấp nhận");
                    break;

                case "BLOCKED_BY_ME":
                    binding.tvStatus.setVisibility(View.VISIBLE);
                    binding.tvStatus.setText("Đã chặn");
                    break;

                case "BLOCKED_ME":
                    // Hide entire item would require DiffUtil; just show locked status
                    binding.tvStatus.setVisibility(View.VISIBLE);
                    binding.tvStatus.setText("Không thể kết bạn");
                    break;

                default:
                    binding.tvStatus.setVisibility(View.VISIBLE);
                    binding.tvStatus.setText(status);
                    break;
            }
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
