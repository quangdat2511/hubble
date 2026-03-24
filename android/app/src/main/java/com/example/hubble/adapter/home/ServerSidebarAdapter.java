package com.example.hubble.adapter.home;

import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hubble.data.model.server.ServerItem;
import com.example.hubble.databinding.ItemServerBinding;
import com.google.android.material.shape.ShapeAppearanceModel;

import java.util.ArrayList;
import java.util.List;

public class ServerSidebarAdapter extends RecyclerView.Adapter<ServerSidebarAdapter.ViewHolder> {

    public interface OnServerClickListener {
        void onServerClick(ServerItem server, int position);
    }

    private final List<ServerItem> servers = new ArrayList<>();
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

    public void setSelectedPosition(int position) {
        if (position < 0 || position >= servers.size()) {
            return;
        }
        int old = selectedPosition;
        selectedPosition = position;
        if (old >= 0 && old < servers.size()) {
            notifyItemChanged(old);
        }
        notifyItemChanged(selectedPosition);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemServerBinding b = ItemServerBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        ViewHolder holder = new ViewHolder(b);

        // Set click listener once during creation for better performance
        holder.itemView.setOnClickListener(v -> {
            int pos = holder.getBindingAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) return;

            // Immediate UI update before callback for instant feedback
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
        holder.bind(server, selected);
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

        void bind(ServerItem server, boolean isSelected) {
            b.viewActiveIndicator.setVisibility(isSelected ? View.VISIBLE : View.GONE);

            // Discord-style: rounded square (16dp) when selected, circle when not
            // Use 16dp for squircle effect like Discord
            float cornerSize = isSelected ? 16f : 999f;
            b.ivServerIcon.setShapeAppearanceModel(
                    ShapeAppearanceModel.builder().setAllCornerSizes(cornerSize).build());

            GradientDrawable bg = new GradientDrawable();
            bg.setShape(GradientDrawable.OVAL);
            bg.setColor(server.getBackgroundColor());
            b.ivServerIcon.setImageDrawable(null);
            b.ivServerIcon.setBackground(bg);

            b.tvServerInitials.setText(server.getInitials());
        }
    }
}


