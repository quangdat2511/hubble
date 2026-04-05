package com.example.hubble.adapter.friend;

import android.graphics.Typeface;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.hubble.R;
import com.example.hubble.data.api.NetworkConfig;
import com.example.hubble.data.model.dm.FriendRequestResponse;
import com.example.hubble.databinding.ItemFriendRequestBinding;
import com.example.hubble.utils.AvatarPlaceholderUtils;

import java.util.ArrayList;
import java.util.List;

public class FriendRequestAdapter extends RecyclerView.Adapter<FriendRequestAdapter.ViewHolder> {

    private final List<FriendRequestResponse> requests = new ArrayList<>();
    private final OnRequestListener listener;

    public interface OnRequestListener {
        void onAccept(FriendRequestResponse request);
        void onDecline(FriendRequestResponse request);
    }

    public FriendRequestAdapter(OnRequestListener listener) {
        this.listener = listener;
    }

    public void setRequests(List<FriendRequestResponse> newRequests) {
        requests.clear();
        if (newRequests != null) requests.addAll(newRequests);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemFriendRequestBinding binding = ItemFriendRequestBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(requests.get(position));
    }

    @Override
    public int getItemCount() {
        return requests.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemFriendRequestBinding binding;

        ViewHolder(ItemFriendRequestBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(FriendRequestResponse request) {
            String name = "";
            if (request.getUser() != null) {
                name = (request.getUser().getDisplayName() != null && !request.getUser().getDisplayName().isEmpty())
                        ? request.getUser().getDisplayName() : request.getUser().getUsername();
            }

            bindAvatar(request, name);

            String fullText = binding.getRoot().getContext()
                    .getString(R.string.notification_friend_request_text, name);
            SpannableString spannable = new SpannableString(fullText);
            if (!name.isEmpty()) {
                spannable.setSpan(new StyleSpan(Typeface.BOLD), 0, name.length(),
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            binding.tvMessage.setText(spannable);
            binding.tvTime.setText(NotificationActivityAdapter.formatRelativeTime(request.getCreatedAt()));

            binding.btnAccept.setOnClickListener(v -> {
                if (listener != null) listener.onAccept(request);
            });
            binding.btnDecline.setOnClickListener(v -> {
                if (listener != null) listener.onDecline(request);
            });
        }

        private void bindAvatar(@Nullable FriendRequestResponse request, String displayName) {
            int avatarSize = binding.ivAvatar.getLayoutParams() != null
                    ? binding.ivAvatar.getLayoutParams().width
                    : binding.ivAvatar.getWidth();
            android.graphics.drawable.Drawable avatarFallback =
                    AvatarPlaceholderUtils.createAvatarDrawable(
                            binding.ivAvatar.getContext(),
                            displayName,
                            avatarSize
                    );

            String avatarUrl = request != null && request.getUser() != null
                    ? toAbsoluteUrl(request.getUser().getAvatarUrl())
                    : null;

            Glide.with(binding.ivAvatar.getContext())
                    .load(avatarUrl)
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
