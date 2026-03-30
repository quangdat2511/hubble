package com.example.hubble.adapter.server;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hubble.R;
import com.example.hubble.data.model.server.ServerInviteResponse;
import com.example.hubble.databinding.ItemPendingInviteBinding;

public class PendingInviteAdapter extends ListAdapter<ServerInviteResponse, PendingInviteAdapter.ViewHolder> {

    public interface OnInviteActionListener {
        void onAccept(ServerInviteResponse invite);
        void onDecline(ServerInviteResponse invite);
    }

    private final OnInviteActionListener listener;

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

    public PendingInviteAdapter(OnInviteActionListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemPendingInviteBinding binding = ItemPendingInviteBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position), listener);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemPendingInviteBinding binding;

        ViewHolder(ItemPendingInviteBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(ServerInviteResponse invite, OnInviteActionListener listener) {
            // Server name
            binding.tvServerName.setText(
                    invite.getServerName() != null ? invite.getServerName() : "");

            // Server icon initials
            String serverName = invite.getServerName();
            String initials = (serverName != null && !serverName.isEmpty())
                    ? serverName.substring(0, 1).toUpperCase() : "?";
            binding.tvServerInitials.setText(initials);

            // Inviter info
            String inviterDisplay = invite.getInviterDisplayName();
            String inviterUsername = invite.getInviterUsername();
            String inviterLabel = inviterDisplay != null && !inviterDisplay.isEmpty()
                    ? inviterDisplay : inviterUsername;
            binding.tvInviterInfo.setText(
                    binding.getRoot().getContext()
                            .getString(R.string.invite_from_inviter, inviterLabel != null ? inviterLabel : ""));

            // Created at
            String createdAt = invite.getCreatedAt();
            if (createdAt != null && createdAt.length() >= 10) {
                binding.tvCreatedAt.setText(
                        binding.getRoot().getContext()
                                .getString(R.string.invite_created_at, createdAt.substring(0, 10)));
            } else {
                binding.tvCreatedAt.setText("");
            }

            // Action buttons
            binding.btnAccept.setOnClickListener(v -> listener.onAccept(invite));
            binding.btnDecline.setOnClickListener(v -> listener.onDecline(invite));
        }
    }
}

