package com.example.hubble.adapter.server;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hubble.data.model.server.ServerMemberItem;
import com.example.hubble.databinding.ItemMemberSelectableBinding;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AddRoleMemberAdapter extends RecyclerView.Adapter<AddRoleMemberAdapter.ViewHolder> {

    public interface OnSelectionChangedListener {
        void onSelectionChanged(int count);
    }

    private List<ServerMemberItem> allMembers = new ArrayList<>();
    private List<ServerMemberItem> filtered = new ArrayList<>();
    private final Set<String> selectedUserIds = new HashSet<>();
    private OnSelectionChangedListener listener;

    public void setOnSelectionChangedListener(OnSelectionChangedListener listener) {
        this.listener = listener;
    }

    public void setMembers(List<ServerMemberItem> members) {
        this.allMembers = new ArrayList<>(members);
        this.filtered = new ArrayList<>(members);
        notifyDataSetChanged();
    }

    public void filter(String query) {
        if (query == null || query.trim().isEmpty()) {
            filtered = new ArrayList<>(allMembers);
        } else {
            String q = query.toLowerCase().trim();
            filtered = new ArrayList<>();
            for (ServerMemberItem m : allMembers) {
                String display = m.getDisplayName() != null ? m.getDisplayName() : "";
                String username = m.getUsername() != null ? m.getUsername() : "";
                if (display.toLowerCase().contains(q) || username.toLowerCase().contains(q)) {
                    filtered.add(m);
                }
            }
        }
        notifyDataSetChanged();
    }

    public List<String> getSelectedUserIds() {
        return new ArrayList<>(selectedUserIds);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemMemberSelectableBinding binding = ItemMemberSelectableBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(filtered.get(position));
    }

    @Override
    public int getItemCount() {
        return filtered.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemMemberSelectableBinding binding;

        ViewHolder(ItemMemberSelectableBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(ServerMemberItem member) {
            String display = member.getDisplayName() != null ? member.getDisplayName() : member.getUsername();
            binding.tvDisplayName.setText(display);
            binding.tvUsername.setText(member.getUsername());
            binding.checkBox.setVisibility(View.VISIBLE);
            binding.checkBox.setChecked(selectedUserIds.contains(member.getUserId()));

            View.OnClickListener toggle = v -> {
                String uid = member.getUserId();
                if (selectedUserIds.contains(uid)) {
                    selectedUserIds.remove(uid);
                } else if (selectedUserIds.size() < 30) {
                    selectedUserIds.add(uid);
                }
                binding.checkBox.setChecked(selectedUserIds.contains(uid));
                if (listener != null) listener.onSelectionChanged(selectedUserIds.size());
            };

            itemView.setOnClickListener(toggle);
            binding.checkBox.setOnClickListener(toggle);
        }
    }
}
