package com.example.hubble.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hubble.R;
import com.example.hubble.data.model.ChannelListItem;
import com.example.hubble.databinding.ItemChannelBinding;
import com.example.hubble.databinding.ItemChannelCategoryBinding;

import java.util.ArrayList;
import java.util.List;

public class ChannelListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public interface OnChannelClickListener {
        void onChannelClick(ChannelListItem.Channel channel);
    }

    private final List<ChannelListItem> allItems = new ArrayList<>();
    private final List<ChannelListItem> visibleItems = new ArrayList<>();
    private OnChannelClickListener listener;

    public void setItems(List<ChannelListItem> items) {
        allItems.clear();
        allItems.addAll(items);
        rebuildVisible();
    }

    public void setOnChannelClickListener(OnChannelClickListener listener) {
        this.listener = listener;
    }

    private void rebuildVisible() {
        visibleItems.clear();
        String collapsedCategoryId = null;
        for (ChannelListItem item : allItems) {
            if (item.getType() == ChannelListItem.TYPE_CATEGORY) {
                ChannelListItem.Category cat = (ChannelListItem.Category) item;
                collapsedCategoryId = cat.isCollapsed() ? cat.getId() : null;
                visibleItems.add(item);
            } else if (item.getType() == ChannelListItem.TYPE_CHANNEL) {
                ChannelListItem.Channel ch = (ChannelListItem.Channel) item;
                if (collapsedCategoryId == null || !collapsedCategoryId.equals(ch.getCategoryId())) {
                    visibleItems.add(item);
                }
            }
        }
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return visibleItems.get(position).getType();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == ChannelListItem.TYPE_CATEGORY) {
            return new CategoryViewHolder(ItemChannelCategoryBinding.inflate(inflater, parent, false));
        } else {
            return new ChannelViewHolder(ItemChannelBinding.inflate(inflater, parent, false));
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChannelListItem item = visibleItems.get(position);
        if (holder instanceof CategoryViewHolder) {
            ((CategoryViewHolder) holder).bind((ChannelListItem.Category) item);
        } else {
            ((ChannelViewHolder) holder).bind((ChannelListItem.Channel) item);
        }
    }

    @Override
    public int getItemCount() { return visibleItems.size(); }

    // ── Category ViewHolder ───────────────────────────────────────────────

    class CategoryViewHolder extends RecyclerView.ViewHolder {
        private final ItemChannelCategoryBinding binding;

        CategoryViewHolder(ItemChannelCategoryBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(ChannelListItem.Category category) {
            binding.tvCategoryName.setText(category.getName());
            // Rotate arrow to indicate collapsed state
            binding.ivCollapseArrow.setRotation(category.isCollapsed() ? -90f : 0f);
            binding.getRoot().setOnClickListener(v -> {
                category.toggleCollapsed();
                rebuildVisible();
            });
        }
    }

    // ── Channel ViewHolder ────────────────────────────────────────────────

    class ChannelViewHolder extends RecyclerView.ViewHolder {
        private final ItemChannelBinding binding;

        ChannelViewHolder(ItemChannelBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(ChannelListItem.Channel channel) {
            binding.tvChannelName.setText(channel.getName());
            // Set icon based on channel type
            if (channel.getChannelType() == ChannelListItem.Channel.ChannelType.VOICE) {
                binding.ivChannelIcon.setImageResource(R.drawable.ic_sound);
            } else {
                binding.ivChannelIcon.setImageResource(R.drawable.ic_hashtag);
            }
            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) listener.onChannelClick(channel);
            });
        }
    }
}
