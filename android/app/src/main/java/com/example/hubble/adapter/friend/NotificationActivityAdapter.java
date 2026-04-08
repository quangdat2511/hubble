package com.example.hubble.adapter.friend;

import android.graphics.Typeface;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hubble.R;
import com.example.hubble.data.model.dm.FriendRequestResponse;
import com.example.hubble.databinding.ItemNotificationActivityBinding;
import com.example.hubble.utils.LocalizedTimeUtils;

import java.util.ArrayList;
import java.util.List;

public class NotificationActivityAdapter extends RecyclerView.Adapter<NotificationActivityAdapter.ViewHolder> {

    private final List<FriendRequestResponse> items = new ArrayList<>();

    public void setItems(List<FriendRequestResponse> newItems) {
        items.clear();
        if (newItems != null) items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemNotificationActivityBinding binding = ItemNotificationActivityBinding.inflate(
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
        private final ItemNotificationActivityBinding binding;

        ViewHolder(ItemNotificationActivityBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(FriendRequestResponse item) {
            String name = "";
            if (item.getUser() != null) {
                name = (item.getUser().getDisplayName() != null && !item.getUser().getDisplayName().isEmpty())
                        ? item.getUser().getDisplayName() : item.getUser().getUsername();
            }

            String fullText = binding.getRoot().getContext()
                    .getString(R.string.notification_friend_accepted_text, name);
            SpannableString spannable = new SpannableString(fullText);
            if (!name.isEmpty()) {
                spannable.setSpan(new StyleSpan(Typeface.BOLD), 0, name.length(),
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            binding.tvMessage.setText(spannable);
            binding.tvTime.setText(LocalizedTimeUtils.formatRelativeTime(
                    binding.getRoot().getContext(),
                    item.getCreatedAt()
            ));
        }
    }
}
