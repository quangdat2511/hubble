package com.example.hubble.adapter.server;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hubble.R;
import com.example.hubble.data.model.server.ServerMemberItem;
import com.example.hubble.data.model.server.ServerRoleItem;
import com.example.hubble.databinding.ItemMembersHeaderBinding;
import com.example.hubble.databinding.ItemMemberRowBinding;

import java.util.ArrayList;
import java.util.List;

public class ServerMemberAdapter extends ListAdapter<ServerMemberAdapter.AdapterItem, RecyclerView.ViewHolder> {
    private static final int VIEW_TYPE_HEADER = 0;
    private static final int VIEW_TYPE_MEMBER = 1;
    private OnMemberClickListener listener;

    public interface OnMemberClickListener {
        void onMemberClick(ServerMemberItem member);
    }

    public static class AdapterItem {
        private String headerText;
        private ServerMemberItem member;

        public AdapterItem(String headerText) {
            this.headerText = headerText;
        }

        public AdapterItem(ServerMemberItem member) {
            this.member = member;
        }

        public String getHeaderText() {
            return headerText;
        }

        public ServerMemberItem getMember() {
            return member;
        }

        public boolean isHeader() {
            return headerText != null;
        }
    }

    private static final DiffUtil.ItemCallback<AdapterItem> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<AdapterItem>() {
                @Override
                public boolean areItemsTheSame(@NonNull AdapterItem oldItem, @NonNull AdapterItem newItem) {
                    if (oldItem.isHeader() && newItem.isHeader()) {
                        return oldItem.getHeaderText().equals(newItem.getHeaderText());
                    }
                    if (!oldItem.isHeader() && !newItem.isHeader()) {
                        return oldItem.getMember().getUserId().equals(newItem.getMember().getUserId());
                    }
                    return false;
                }

                @Override
                public boolean areContentsTheSame(@NonNull AdapterItem oldItem, @NonNull AdapterItem newItem) {
                    return areItemsTheSame(oldItem, newItem);
                }
            };

    public ServerMemberAdapter(OnMemberClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        AdapterItem item = getItem(position);
        return item.isHeader() ? VIEW_TYPE_HEADER : VIEW_TYPE_MEMBER;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_HEADER) {
            ItemMembersHeaderBinding binding = ItemMembersHeaderBinding.inflate(
                    LayoutInflater.from(parent.getContext()), parent, false);
            return new HeaderViewHolder(binding);
        } else {
            ItemMemberRowBinding binding = ItemMemberRowBinding.inflate(
                    LayoutInflater.from(parent.getContext()), parent, false);
            return new MemberViewHolder(binding);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        AdapterItem item = getItem(position);
        if (holder instanceof HeaderViewHolder) {
            ((HeaderViewHolder) holder).bind(item.getHeaderText());
        } else {
            ((MemberViewHolder) holder).bind(item.getMember(), listener);
        }
    }

    public static class HeaderViewHolder extends RecyclerView.ViewHolder {
        private ItemMembersHeaderBinding binding;

        public HeaderViewHolder(@NonNull ItemMembersHeaderBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(String headerText) {
            binding.tvSectionLabel.setText(headerText);
        }
    }

    public static class MemberViewHolder extends RecyclerView.ViewHolder {
        private ItemMemberRowBinding binding;

        public MemberViewHolder(@NonNull ItemMemberRowBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(ServerMemberItem member, OnMemberClickListener listener) {
            // Username
            String displayName = member.getDisplayName() != null && !member.getDisplayName().isEmpty() ?
                    member.getDisplayName() : member.getUsername();
            binding.tvUsername.setText(displayName);

            // Avatar
            if (member.getAvatarUrl() != null && !member.getAvatarUrl().isEmpty()) {
                binding.ivAvatar.setVisibility(android.view.View.VISIBLE);
                binding.tvAvatarInitials.setVisibility(android.view.View.GONE);
                // TODO: Load with Glide when implemented
            } else {
                binding.ivAvatar.setVisibility(android.view.View.GONE);
                binding.tvAvatarInitials.setVisibility(android.view.View.VISIBLE);
                binding.tvAvatarInitials.setText(member.getDisplayInitials());
                binding.tvAvatarInitials.setBackgroundColor(member.getAvatarBackgroundColor());
            }

            // Online status
            int statusColor = itemView.getContext().getColor(
                    "ONLINE".equalsIgnoreCase(member.getStatus()) ? R.color.color_online :
                    "IDLE".equalsIgnoreCase(member.getStatus()) ? R.color.color_idle :
                    "DND".equalsIgnoreCase(member.getStatus()) ? R.color.color_dnd :
                    R.color.color_offline
            );
            if ("OFFLINE".equalsIgnoreCase(member.getStatus())) {
                binding.viewOnlineStatus.setVisibility(android.view.View.GONE);
            } else {
                binding.viewOnlineStatus.setVisibility(android.view.View.VISIBLE);
                binding.viewOnlineStatus.setBackgroundTintList(
                        android.content.res.ColorStateList.valueOf(statusColor));
            }

            // Role chips
            binding.llRoleChips.removeAllViews();
            for (ServerRoleItem role : member.getRoles()) {
                TextView chip = new TextView(itemView.getContext());
                chip.setText(role.getName());
                chip.setTextSize(12);
                chip.setTextColor(0xFFF2F3F5); // Discord text color

                // Role background with 30% opacity
                int roleColor = role.getColor();
                int r = Color.red(roleColor);
                int g = Color.green(roleColor);
                int b = Color.blue(roleColor);
                int translucentColor = Color.argb(77, r, g, b);

                chip.setBackgroundColor(translucentColor);

                int padding = itemView.getContext().getResources().getDimensionPixelSize(R.dimen.spacing_xs);
                chip.setPadding(padding, padding / 2, padding, padding / 2);

                android.view.ViewGroup.MarginLayoutParams params = new android.widget.LinearLayout.LayoutParams(
                        android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                        android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
                params.setMarginEnd(padding);
                chip.setLayoutParams(params);

                binding.llRoleChips.addView(chip);
            }

            // Click listener
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onMemberClick(member);
                }
            });
        }
    }
}
