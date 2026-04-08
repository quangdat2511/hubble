package com.example.hubble.adapter.server;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import android.graphics.Typeface;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.StyleSpan;

import com.example.hubble.R;
import com.example.hubble.data.model.server.ServerInviteResponse;
import com.example.hubble.databinding.ItemPendingInviteBinding;
import com.example.hubble.utils.LocalizedTimeUtils;

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
            // Server initials for icon
            String serverName = invite.getServerName() != null ? invite.getServerName() : "";
            String initials = serverName.isEmpty() ? "?" : serverName.substring(0, 1).toUpperCase();
            binding.tvServerInitials.setText(initials);

            // Notification text: "[Inviter] đã mời bạn tham gia [ServerName]."
            String inviterDisplay = invite.getInviterDisplayName();
            String inviterUsername = invite.getInviterUsername();
            String inviterName = (inviterDisplay != null && !inviterDisplay.isEmpty())
                    ? inviterDisplay : (inviterUsername != null ? inviterUsername : "");

            String fullText = binding.getRoot().getContext()
                    .getString(R.string.notification_server_invite_text, inviterName, serverName);
            SpannableString spannable = new SpannableString(fullText);
            if (!inviterName.isEmpty()) {
                spannable.setSpan(new StyleSpan(Typeface.BOLD), 0, inviterName.length(),
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            int serverStart = fullText.indexOf(serverName);
            if (serverStart >= 0 && !serverName.isEmpty()) {
                spannable.setSpan(new StyleSpan(Typeface.BOLD), serverStart,
                        serverStart + serverName.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            binding.tvMessage.setText(spannable);

            binding.tvTime.setText(LocalizedTimeUtils.formatRelativeTime(
                    binding.getRoot().getContext(),
                    invite.getCreatedAt()
            ));

            // Action buttons
            binding.btnAccept.setOnClickListener(v -> listener.onAccept(invite));
            binding.btnDecline.setOnClickListener(v -> listener.onDecline(invite));
        }
    }
}
