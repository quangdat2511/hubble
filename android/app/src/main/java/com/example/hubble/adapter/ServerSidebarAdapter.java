package com.example.hubble.adapter;

import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hubble.data.model.ServerItem;
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
        servers.clear();
        servers.addAll(items);
        notifyDataSetChanged();
    }

    public void setOnServerClickListener(OnServerClickListener l) {
        this.listener = l;
    }

    public void setSelectedPosition(int position) {
        int old = selectedPosition;
        selectedPosition = position;
        notifyItemChanged(old);
        notifyItemChanged(selectedPosition);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemServerBinding b = ItemServerBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(b);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ServerItem server = servers.get(position);
        boolean selected = (position == selectedPosition);
        holder.bind(server, selected);
        holder.itemView.setOnClickListener(v -> {
            int pos = holder.getBindingAdapterPosition();
            if (pos == RecyclerView.NO_ID) return;
            setSelectedPosition(pos);
            if (listener != null) listener.onServerClick(servers.get(pos), pos);
        });
    }

    @Override
    public int getItemCount() { return servers.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemServerBinding b;

        ViewHolder(ItemServerBinding binding) {
            super(binding.getRoot());
            this.b = binding;
        }

        void bind(ServerItem server, boolean isSelected) {
            b.viewActiveIndicator.setVisibility(isSelected ? View.VISIBLE : View.GONE);

            // Discord-style: squircle when selected, circle when not
            float corner = isSelected ? 48f : 999f;
            b.ivServerIcon.setShapeAppearanceModel(
                    ShapeAppearanceModel.builder().setAllCornerSizes(corner).build());

            GradientDrawable bg = new GradientDrawable();
            bg.setShape(GradientDrawable.OVAL);
            bg.setColor(server.getBackgroundColor());
            b.ivServerIcon.setImageDrawable(null);
            b.ivServerIcon.setBackground(bg);

            b.tvServerInitials.setText(server.getInitials());
        }
    }
}
