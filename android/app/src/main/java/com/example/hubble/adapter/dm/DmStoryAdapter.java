package com.example.hubble.adapter.dm;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hubble.data.model.dm.DmConversationItem;
import com.example.hubble.databinding.ItemDmStoryBinding;

import java.util.ArrayList;
import java.util.List;

public class DmStoryAdapter extends RecyclerView.Adapter<DmStoryAdapter.ViewHolder> {

    public interface OnStoryClickListener {
        void onStoryClick(DmConversationItem item);
    }

    private final List<DmConversationItem> items = new ArrayList<>();
    private OnStoryClickListener listener;

    public void setItems(List<DmConversationItem> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    public void setOnStoryClickListener(OnStoryClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemDmStoryBinding binding = ItemDmStoryBinding.inflate(
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
        private final ItemDmStoryBinding binding;

        ViewHolder(ItemDmStoryBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(DmConversationItem item, OnStoryClickListener listener) {
            String displayName = item.getDisplayName() != null ? item.getDisplayName().trim() : "";
            String initial = displayName.isEmpty() ? "?" : displayName.substring(0, 1).toUpperCase();
            binding.tvInitial.setText(initial);
            binding.viewPresence.setVisibility(item.isOnline() ? View.VISIBLE : View.GONE);
            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) {
                    listener.onStoryClick(item);
                }
            });
        }
    }
}


