package com.example.hubble.adapter.server;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hubble.R;
import com.example.hubble.data.model.server.ServerInviteResponse;
import com.example.hubble.databinding.ItemServerInviteBinding;

public class ServerInviteAdapter extends ListAdapter<ServerInviteResponse, ServerInviteAdapter.ViewHolder> {

    private static final DiffUtil.ItemCallback<ServerInviteResponse> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<ServerInviteResponse>() {
                @Override
                public boolean areItemsTheSame(@NonNull ServerInviteResponse o,
                                               @NonNull ServerInviteResponse n) {
                    return o.getId() != null && o.getId().equals(n.getId());
                }

                @Override
                public boolean areContentsTheSame(@NonNull ServerInviteResponse o,
                                                   @NonNull ServerInviteResponse n) {
                    return o.getId().equals(n.getId())
                            && nullSafeEquals(o.getStatus(), n.getStatus());
                }

                private boolean nullSafeEquals(String a, String b) {
                    return a == null ? b == null : a.equals(b);
                }
            };

    public ServerInviteAdapter() {
        super(DIFF_CALLBACK);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemServerInviteBinding binding = ItemServerInviteBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemServerInviteBinding binding;

        ViewHolder(ItemServerInviteBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(ServerInviteResponse invite) {
            // Invitee display name / username
            String displayName = invite.getInviteeDisplayName();
            String username = invite.getInviteeUsername();
            binding.tvDisplayName.setText(
                    displayName != null && !displayName.isEmpty() ? displayName : username);
            binding.tvUsername.setText(username != null ? "@" + username : "");

            // Avatar initials
            String initials = resolveInitials(displayName, username);
            binding.tvAvatarInitials.setText(initials);
            binding.tvAvatarInitials.setVisibility(View.VISIBLE);

            // Status chip
            String status = invite.getStatus();
            binding.tvStatus.setText(localizeStatus(status));
            binding.tvStatus.setChipBackgroundColorResource(chipColorRes(status));

            // Created at
            String createdAt = invite.getCreatedAt();
            if (createdAt != null && createdAt.length() >= 10) {
                binding.tvCreatedAt.setText(binding.getRoot().getContext()
                        .getString(R.string.invite_created_at, createdAt.substring(0, 10)));
                binding.tvCreatedAt.setVisibility(View.VISIBLE);
            } else {
                binding.tvCreatedAt.setVisibility(View.GONE);
            }
        }

        private String resolveInitials(String displayName, String username) {
            if (displayName != null && !displayName.isEmpty())
                return displayName.substring(0, 1).toUpperCase();
            if (username != null && !username.isEmpty())
                return username.substring(0, 1).toUpperCase();
            return "?";
        }

        private String localizeStatus(String status) {
            if (status == null) return "";
            switch (status.toUpperCase()) {
                case "PENDING":   return "Chờ";
                case "ACCEPTED":  return "Đã chấp nhận";
                case "DECLINED":  return "Đã từ chối";
                case "EXPIRED":   return "Hết hạn";
                default:          return status;
            }
        }

        private int chipColorRes(String status) {
            if (status == null) return R.color.color_offline;
            switch (status.toUpperCase()) {
                case "ACCEPTED": return R.color.color_online;
                case "DECLINED":
                case "EXPIRED":  return R.color.color_dnd;
                default:         return R.color.color_idle; // PENDING
            }
        }
    }
}


