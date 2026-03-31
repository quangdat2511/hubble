package com.example.hubble.adapter.server;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hubble.data.model.server.RolePermissionItem;
import com.example.hubble.data.model.server.RolePermissionSection;
import com.example.hubble.databinding.ItemPermissionSectionHeaderBinding;
import com.example.hubble.databinding.ItemPermissionToggleBinding;

import java.util.ArrayList;
import java.util.List;

public class RolePermissionAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_PERMISSION = 1;

    private final List<Object> items = new ArrayList<>();

    public RolePermissionAdapter(List<RolePermissionSection> sections) {
        buildItems(sections);
    }

    private void buildItems(List<RolePermissionSection> sections) {
        items.clear();
        for (RolePermissionSection section : sections) {
            items.add(section.getTitle());
            List<RolePermissionItem> perms = section.getPermissions();
            for (int i = 0; i < perms.size(); i++) {
                items.add(new PermissionWithMeta(perms.get(i), i == perms.size() - 1));
            }
        }
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position) instanceof String ? TYPE_HEADER : TYPE_PERMISSION;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_HEADER) {
            return new HeaderViewHolder(
                    ItemPermissionSectionHeaderBinding.inflate(inflater, parent, false));
        }
        return new PermissionViewHolder(
                ItemPermissionToggleBinding.inflate(inflater, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof HeaderViewHolder) {
            ((HeaderViewHolder) holder).bind((String) items.get(position));
        } else if (holder instanceof PermissionViewHolder) {
            ((PermissionViewHolder) holder).bind((PermissionWithMeta) items.get(position));
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        private final ItemPermissionSectionHeaderBinding binding;

        HeaderViewHolder(ItemPermissionSectionHeaderBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(String title) {
            binding.tvSectionTitle.setText(title);
        }
    }

    static class PermissionViewHolder extends RecyclerView.ViewHolder {
        private final ItemPermissionToggleBinding binding;

        PermissionViewHolder(ItemPermissionToggleBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(PermissionWithMeta item) {
            RolePermissionItem perm = item.permission;
            binding.tvPermName.setText(perm.getName());
            binding.tvPermDescription.setText(perm.getDescription());
            binding.switchPerm.setOnCheckedChangeListener(null);
            binding.switchPerm.setChecked(perm.isEnabled());
            binding.switchPerm.setOnCheckedChangeListener(
                    (buttonView, isChecked) -> perm.setEnabled(isChecked));
            binding.divider.setVisibility(item.isLast ? View.GONE : View.VISIBLE);
        }
    }

    /** Wraps a permission item with positional metadata for divider visibility. */
    private static class PermissionWithMeta {
        final RolePermissionItem permission;
        final boolean isLast;

        PermissionWithMeta(RolePermissionItem permission, boolean isLast) {
            this.permission = permission;
            this.isLast = isLast;
        }
    }
}
