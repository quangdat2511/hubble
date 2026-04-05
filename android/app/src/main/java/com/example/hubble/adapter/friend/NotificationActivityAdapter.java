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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

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
            binding.tvTime.setText(formatRelativeTime(item.getCreatedAt()));
        }
    }

    public static String formatRelativeTime(String isoDate) {
        if (isoDate == null || isoDate.isEmpty()) return "";
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date date = sdf.parse(isoDate.length() > 19 ? isoDate.substring(0, 19) : isoDate);
            if (date == null) return "";
            long diffMs = System.currentTimeMillis() - date.getTime();
            long secs = diffMs / 1000;
            if (secs < 60) return secs + "s";
            long mins = secs / 60;
            if (mins < 60) return mins + "ph";
            long hours = mins / 60;
            if (hours < 24) return hours + "g";
            long days = hours / 24;
            if (days < 7) return days + "ng";
            return (days / 7) + "tuần";
        } catch (Exception e) {
            return "";
        }
    }
}
