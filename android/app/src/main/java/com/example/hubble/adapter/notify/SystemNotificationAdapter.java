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
import com.example.hubble.utils.LocalizedTimeUtils;
import com.example.hubble.utils.NotificationTextFormatter;

import java.util.ArrayList;
import java.util.List;

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
            binding.tvContent.setText(NotificationTextFormatter.format(
                    binding.getRoot().getContext(),
                    item
            ));
            binding.tvContent.setTypeface(null, Boolean.TRUE.equals(item.getIsRead()) ? Typeface.NORMAL : Typeface.BOLD);
            binding.tvTime.setText(LocalizedTimeUtils.formatRelativeTime(
                    binding.getRoot().getContext(),
                    item.getCreatedAt()
            ));
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
}
