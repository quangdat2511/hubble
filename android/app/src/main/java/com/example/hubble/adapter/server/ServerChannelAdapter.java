package com.example.hubble.adapter.server;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hubble.data.model.dm.ChannelDto;
import com.example.hubble.databinding.ItemChannelBinding;
import com.example.hubble.databinding.ItemChannelCategoryBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ServerChannelAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_CATEGORY = 0;
    private static final int VIEW_TYPE_TEXT_CHANNEL = 1;
    private static final int VIEW_TYPE_VOICE_CHANNEL = 2;

    private List<ChannelDto> allChannels = new ArrayList<>();
    private Set<String> collapsedCategories;
    private List<Object> visibleItems = new ArrayList<>(); // ChannelDto items

    private final OnChannelClickListener onChannelClick;
    private final OnCategoryToggleListener onCategoryToggle;

    public interface OnChannelClickListener {
        void onChannelClick(ChannelDto channel);
    }

    public interface OnCategoryToggleListener {
        void onCategoryToggle(String categoryId);
    }

    public ServerChannelAdapter(OnChannelClickListener onChannelClick,
                               OnCategoryToggleListener onCategoryToggle) {
        this.onChannelClick = onChannelClick;
        this.onCategoryToggle = onCategoryToggle;
    }

    public void submitChannels(List<ChannelDto> channels, Set<String> collapsed) {
        this.allChannels = new ArrayList<>(channels != null ? channels : new ArrayList<>());
        this.collapsedCategories = collapsed;
        rebuildVisibleList();
        notifyDataSetChanged();
    }

    private void rebuildVisibleList() {
        visibleItems.clear();

        if (allChannels.isEmpty()) {
            return;
        }

        // 1. Add uncategorized channels first (parentId == null and type != "CATEGORY")
        for (ChannelDto channel : allChannels) {
            if (channel.getParentId() == null && !isCategory(channel)) {
                visibleItems.add(channel);
            }
        }

        // 2. Sort categories by position and add with their children
        List<ChannelDto> categories = new ArrayList<>();
        for (ChannelDto channel : allChannels) {
            if (isCategory(channel)) {
                categories.add(channel);
            }
        }
        categories.sort((a, b) -> Short.compare(a.getPosition() != null ? a.getPosition() : 0,
                                                 b.getPosition() != null ? b.getPosition() : 0));

        for (ChannelDto category : categories) {
            visibleItems.add(category);

            // If category not collapsed, add its children
            boolean collapsed = collapsedCategories != null && collapsedCategories.contains(category.getId());
            if (!collapsed) {
                List<ChannelDto> children = new ArrayList<>();
                for (ChannelDto channel : allChannels) {
                    if (channel.getParentId() != null && channel.getParentId().equals(category.getId())) {
                        children.add(channel);
                    }
                }
                children.sort((a, b) -> Short.compare(a.getPosition() != null ? a.getPosition() : 0,
                                                       b.getPosition() != null ? b.getPosition() : 0));
                visibleItems.addAll(children);
            }
        }
    }

    private boolean isCategory(ChannelDto channel) {
        return "CATEGORY".equalsIgnoreCase(channel.getType());
    }

    private int getChannelViewType(ChannelDto channel) {
        if (isCategory(channel)) {
            return VIEW_TYPE_CATEGORY;
        }
        if ("TEXT".equalsIgnoreCase(channel.getType())) {
            return VIEW_TYPE_TEXT_CHANNEL;
        }
        return VIEW_TYPE_VOICE_CHANNEL;
    }

    @Override
    public int getItemViewType(int position) {
        if (position >= visibleItems.size()) {
            return VIEW_TYPE_TEXT_CHANNEL;
        }
        ChannelDto item = (ChannelDto) visibleItems.get(position);
        return getChannelViewType(item);
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        if (viewType == VIEW_TYPE_CATEGORY) {
            ItemChannelCategoryBinding binding = ItemChannelCategoryBinding.inflate(inflater, parent, false);
            return new CategoryViewHolder(binding);
        } else {
            ItemChannelBinding binding = ItemChannelBinding.inflate(inflater, parent, false);
            return new ChannelViewHolder(binding, viewType);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (position >= visibleItems.size()) {
            return;
        }

        ChannelDto item = (ChannelDto) visibleItems.get(position);

        if (holder instanceof CategoryViewHolder) {
            ((CategoryViewHolder) holder).bind(item);
        } else if (holder instanceof ChannelViewHolder) {
            ((ChannelViewHolder) holder).bind(item);
        }
    }

    @Override
    public int getItemCount() {
        return visibleItems.size();
    }

    private class CategoryViewHolder extends RecyclerView.ViewHolder {
        private final ItemChannelCategoryBinding binding;

        CategoryViewHolder(ItemChannelCategoryBinding binding) {
            super(binding.getRoot());
            this.binding = binding;

            itemView.setOnClickListener(v -> {
                int pos = getBindingAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && pos < visibleItems.size()) {
                    ChannelDto category = (ChannelDto) visibleItems.get(pos);
                    onCategoryToggle.onCategoryToggle(category.getId());
                }
            });
        }

        void bind(ChannelDto category) {
            binding.tvCategoryName.setText(category.getName());

            boolean collapsed = collapsedCategories != null && collapsedCategories.contains(category.getId());
            float rotation = collapsed ? -90f : 0f;
            binding.ivCollapseArrow.setRotation(rotation);
        }
    }

    private class ChannelViewHolder extends RecyclerView.ViewHolder {
        private final ItemChannelBinding binding;
        private final int viewType;

        ChannelViewHolder(ItemChannelBinding binding, int viewType) {
            super(binding.getRoot());
            this.binding = binding;
            this.viewType = viewType;

            itemView.setOnClickListener(v -> {
                int pos = getBindingAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && pos < visibleItems.size()) {
                    ChannelDto channel = (ChannelDto) visibleItems.get(pos);
                    onChannelClick.onChannelClick(channel);
                }
            });
        }

        void bind(ChannelDto channel) {
            binding.tvChannelName.setText(channel.getName());

            // Set icon based on channel type
            if (viewType == VIEW_TYPE_TEXT_CHANNEL) {
                binding.ivChannelIcon.setImageResource(com.example.hubble.R.drawable.ic_hashtag);
            } else {
                binding.ivChannelIcon.setImageResource(com.example.hubble.R.drawable.ic_sound);
            }
        }
    }
}
