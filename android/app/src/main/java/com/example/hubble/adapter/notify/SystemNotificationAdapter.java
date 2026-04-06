package com.example.hubble.adapter.notify;

import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hubble.R;
import com.example.hubble.data.model.notify.NotificationResponse;
import com.example.hubble.databinding.ItemSystemNotificationBinding;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class SystemNotificationAdapter extends RecyclerView.Adapter<SystemNotificationAdapter.ViewHolder> {

    public interface OnNotificationClickListener {
        void onClick(NotificationResponse notification);
    }

    private final List<NotificationResponse> items = new ArrayList<>();
    private OnNotificationClickListener listener;

    public void setListener(OnNotificationClickListener listener) {
        this.listener = listener;
    }

    public void setItems(List<NotificationResponse> newItems) {
        items.clear();
        if (newItems != null) items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemSystemNotificationBinding binding = ItemSystemNotificationBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(items.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemSystemNotificationBinding binding;

        ViewHolder(ItemSystemNotificationBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(NotificationResponse item, OnNotificationClickListener listener) {
            binding.tvContent.setText(item.getContent());
            binding.tvContent.setTypeface(null, Boolean.TRUE.equals(item.getIsRead()) ? Typeface.NORMAL : Typeface.BOLD);
            binding.tvTime.setText(formatRelativeTime(item.getCreatedAt()));
            binding.ivUnreadDot.setVisibility(Boolean.TRUE.equals(item.getIsRead()) ? View.GONE : View.VISIBLE);

            int iconRes;
            switch (item.getType() != null ? item.getType() : "") {
                case "FRIEND_REQUEST":
                    iconRes = R.drawable.ic_person_add;
                    break;
                case "SERVER_INVITE":
                    iconRes = R.drawable.ic_group_add;
                    break;
                default:
                    iconRes = R.drawable.ic_notifications;
                    break;
            }
            binding.ivIcon.setImageResource(iconRes);

            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) listener.onClick(item);
            });
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
