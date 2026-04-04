package com.example.hubble.adapter.home;

import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.hubble.data.model.server.ServerItem;
import com.example.hubble.databinding.ItemServerBinding;
import com.google.android.material.shape.RelativeCornerSize;
import com.google.android.material.shape.ShapeAppearanceModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ServerSidebarAdapter extends RecyclerView.Adapter<ServerSidebarAdapter.ViewHolder> {

    public interface OnServerClickListener {
        void onServerClick(ServerItem server, int position);
    }

    private final List<ServerItem> servers = new ArrayList<>();
    private final Map<String, Integer> unreadByServerId = new HashMap<>();
    private int selectedPosition = 0;
    private OnServerClickListener listener;

    public void setServers(List<ServerItem> items) {
        List<ServerItem> newItems = items != null ? new ArrayList<>(items) : new ArrayList<>();
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() {
                return servers.size();
            }

            @Override
            public int getNewListSize() {
                return newItems.size();
            }

            @Override
            public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                String oldId = servers.get(oldItemPosition).getId();
                String newId = newItems.get(newItemPosition).getId();
                return oldId != null && oldId.equals(newId);
            }

            @Override
            public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                ServerItem oldItem = servers.get(oldItemPosition);
                ServerItem newItem = newItems.get(newItemPosition);
                return oldItem.getBackgroundColor() == newItem.getBackgroundColor()
                        && stringEquals(oldItem.getName(), newItem.getName())
                        && stringEquals(oldItem.getIconUrl(), newItem.getIconUrl());
            }
        });

        servers.clear();
        servers.addAll(newItems);
        if (servers.isEmpty()) {
            selectedPosition = 0;
        } else if (selectedPosition >= servers.size()) {
            selectedPosition = 0;
        }
        diffResult.dispatchUpdatesTo(this);
    }

    public void setOnServerClickListener(OnServerClickListener l) {
        this.listener = l;
    }

    public void setUnreadByServerId(Map<String, Integer> map) {
        unreadByServerId.clear();
        if (map != null) {
            unreadByServerId.putAll(map);
        }
        notifyDataSetChanged();
    }

    public void setSelectedPosition(int position) {
        int old = selectedPosition;
        if (old == position) return;
        selectedPosition = position;
        if (old >= 0 && old < servers.size()) notifyItemChanged(old);
        if (position >= 0 && position < servers.size()) notifyItemChanged(position);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemServerBinding b = ItemServerBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        ViewHolder holder = new ViewHolder(b);

        holder.itemView.setOnClickListener(v -> {
            int pos = holder.getBindingAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) return;
            if (pos != selectedPosition) {
                int old = selectedPosition;
                selectedPosition = pos;
                notifyItemChanged(old);
                notifyItemChanged(selectedPosition);
            }
            if (listener != null) listener.onServerClick(servers.get(pos), pos);
        });

        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ServerItem server = servers.get(position);
        boolean selected = (position == selectedPosition);
        int unread = 0;
        if (server.getId() != null) {
            Integer u = unreadByServerId.get(server.getId());
            unread = u != null ? u : 0;
        }
        holder.bind(server, selected, unread);
    }

    @Override
    public int getItemCount() { return servers.size(); }

    private boolean stringEquals(String a, String b) {
        if (a == null) {
            return b == null;
        }
        return a.equals(b);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemServerBinding b;

        ViewHolder(ItemServerBinding binding) {
            super(binding.getRoot());
            this.b = binding;
        }

        void bind(ServerItem server, boolean isSelected, int unreadCount) {
            b.viewActiveIndicator.setVisibility(isSelected ? View.VISIBLE : View.GONE);
            boolean showUnreadPill = unreadCount > 0 && !isSelected;
            b.viewUnreadPill.setVisibility(showUnreadPill ? View.VISIBLE : View.GONE);

            if (unreadCount > 0) {
                b.tvServerUnreadBadge.setVisibility(View.VISIBLE);
                b.tvServerUnreadBadge.setText(unreadCount > 99 ? "99+" : String.valueOf(unreadCount));
            } else {
                b.tvServerUnreadBadge.setVisibility(View.GONE);
            }

            float density = b.ivServerIcon.getContext().getResources().getDisplayMetrics().density;
            ShapeAppearanceModel shapeModel = isSelected
                    ? ShapeAppearanceModel.builder().setAllCornerSizes(16f * density).build()
                    : ShapeAppearanceModel.builder().setAllCornerSizes(new RelativeCornerSize(0.5f)).build();
            b.ivServerIcon.setShapeAppearanceModel(shapeModel);

            if (server.getIconUrl() != null && !server.getIconUrl().isEmpty()) {
                b.tvServerInitials.setVisibility(View.GONE);
                Glide.with(b.ivServerIcon.getContext())
                        .load(server.getIconUrl())
                        .centerCrop()
                        .into(b.ivServerIcon);
            } else {
                Glide.with(b.ivServerIcon.getContext()).clear(b.ivServerIcon);
                b.ivServerIcon.setImageDrawable(new ColorDrawable(server.getBackgroundColor()));
                b.tvServerInitials.setVisibility(View.VISIBLE);
                b.tvServerInitials.setText(server.getInitials());
            }
        }
    }
}
