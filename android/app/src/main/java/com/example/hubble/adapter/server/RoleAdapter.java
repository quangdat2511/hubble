package com.example.hubble.adapter.server;

import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hubble.R;
import com.example.hubble.data.model.server.ServerRoleItem;
import com.example.hubble.databinding.ItemRoleBinding;

import java.util.List;

public class RoleAdapter extends RecyclerView.Adapter<RoleAdapter.RoleViewHolder> {

    private final List<ServerRoleItem> roles;
    private final OnRoleClickListener listener;

    public interface OnRoleClickListener {
        void onRoleClick(ServerRoleItem role);
    }

    public RoleAdapter(List<ServerRoleItem> roles, OnRoleClickListener listener) {
        this.roles = roles;
        this.listener = listener;
    }

    @NonNull
    @Override
    public RoleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemRoleBinding binding = ItemRoleBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new RoleViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull RoleViewHolder holder, int position) {
        holder.bind(roles.get(position));
    }

    @Override
    public int getItemCount() {
        return roles.size();
    }

    class RoleViewHolder extends RecyclerView.ViewHolder {
        private final ItemRoleBinding binding;

        RoleViewHolder(ItemRoleBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(ServerRoleItem role) {
            binding.tvRoleName.setText(role.getName());
            binding.tvRoleMemberCount.setText(
                    itemView.getContext().getString(R.string.roles_member_count, role.getMemberCount()));

            // Set role color circle
            GradientDrawable circle = new GradientDrawable();
            circle.setShape(GradientDrawable.OVAL);
            circle.setColor(role.getColor());
            binding.viewRoleColor.setBackground(circle);

            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onRoleClick(role);
            });
        }
    }
}
