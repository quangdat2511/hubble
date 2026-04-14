package com.example.hubble.adapter.server;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hubble.data.model.server.ServerMemberItem;
import com.example.hubble.databinding.ItemMemberSelectableBinding;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MemberSelectAdapter extends RecyclerView.Adapter<MemberSelectAdapter.ViewHolder> {

    private List<ServerMemberItem> members;
    private List<ServerMemberItem> allMembers;
    private final Set<String> selectedIds = new HashSet<>();
    private final OnSelectionChangeListener listener;

    public interface OnSelectionChangeListener {
        void onSelectionChanged(int selectedCount);
    }

    public MemberSelectAdapter(List<ServerMemberItem> members, OnSelectionChangeListener listener) {
        this.members = members;
        this.allMembers = new ArrayList<>(members);
        this.listener = listener;
    }

    public void updateList(List<ServerMemberItem> newMembers) {
        this.members = new ArrayList<>(newMembers);
        this.allMembers = new ArrayList<>(newMembers);
        notifyDataSetChanged();
    }

    public void filter(String query) {
        if (query == null || query.trim().isEmpty()) {
            members = new ArrayList<>(allMembers);
        } else {
            String lower = query.toLowerCase().trim();
            List<ServerMemberItem> filtered = new ArrayList<>();
            for (ServerMemberItem m : allMembers) {
                if ((m.getDisplayName() != null && m.getDisplayName().toLowerCase().contains(lower))
                        || (m.getUsername() != null && m.getUsername().toLowerCase().contains(lower))) {
                    filtered.add(m);
                }
            }
            members = filtered;
        }
        notifyDataSetChanged();
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
        holder.bind(members.get(position));
    }

    @Override
    public int getItemCount() {
        return members.size();
    }

    public Set<String> getSelectedIds() {
        return selectedIds;
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemMemberSelectableBinding binding;

        ViewHolder(ItemMemberSelectableBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(ServerMemberItem member) {
            binding.tvDisplayName.setText(member.getDisplayName());
            binding.tvUsername.setText(member.getUsername());

            binding.checkBox.setOnCheckedChangeListener(null);
            binding.checkBox.setChecked(selectedIds.contains(member.getUserId()));
            binding.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    selectedIds.add(member.getUserId());
                } else {
                    selectedIds.remove(member.getUserId());
                }
                if (listener != null) listener.onSelectionChanged(selectedIds.size());
            });

            itemView.setOnClickListener(v -> binding.checkBox.toggle());
        }
    }
}
