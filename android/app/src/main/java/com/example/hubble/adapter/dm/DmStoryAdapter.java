package com.example.hubble.adapter.dm;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hubble.data.model.dm.DmConversationItem;
import com.example.hubble.databinding.ItemDmStoryBinding;
import com.example.hubble.utils.AvatarUtils;

import java.util.ArrayList;
import java.util.List;

public class DmStoryAdapter extends RecyclerView.Adapter<DmStoryAdapter.ViewHolder> {

    private final List<DmConversationItem> items = new ArrayList<>();

    public void setItems(List<DmConversationItem> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
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
        holder.bind(items.get(position));
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

        void bind(DmConversationItem item) {
            binding.tvInitial.setText(item.getDisplayName().substring(0, 1).toUpperCase());
            AvatarUtils.loadAvatar(binding.getRoot(), binding.ivAvatar, binding.tvInitial, item.getAvatarUrl());
            binding.viewPresence.setVisibility(item.isOnline() ? View.VISIBLE : View.GONE);
        }
    }
}


