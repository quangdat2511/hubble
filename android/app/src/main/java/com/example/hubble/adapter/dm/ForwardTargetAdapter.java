package com.example.hubble.adapter.dm;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hubble.databinding.ItemForwardTargetBinding;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class ForwardTargetAdapter extends RecyclerView.Adapter<ForwardTargetAdapter.ViewHolder> {

    public static class TargetItem {
        public final String channelId;
        public final String title;
        public final String subtitle;

        public TargetItem(String channelId, String title, String subtitle) {
            this.channelId = channelId;
            this.title = title;
            this.subtitle = subtitle;
        }
    }

    public interface OnSelectionChangedListener {
        void onSelectionChanged(Set<String> selectedIds);
    }

    private final List<TargetItem> allItems = new ArrayList<>();
    private final List<TargetItem> filteredItems = new ArrayList<>();
    private final Set<String> selectedIds = new HashSet<>();
    private final OnSelectionChangedListener selectionChangedListener;

    public ForwardTargetAdapter(OnSelectionChangedListener selectionChangedListener) {
        this.selectionChangedListener = selectionChangedListener;
    }

    public void setItems(List<TargetItem> items) {
        allItems.clear();
        if (items != null) {
            allItems.addAll(items);
        }
        applyFilter("");
    }

    public void applyFilter(String query) {
        filteredItems.clear();
        String q = query == null ? "" : query.trim().toLowerCase(Locale.getDefault());
        for (TargetItem item : allItems) {
            boolean match = q.isEmpty()
                    || (item.title != null && item.title.toLowerCase(Locale.getDefault()).contains(q))
                    || (item.subtitle != null && item.subtitle.toLowerCase(Locale.getDefault()).contains(q));
            if (match) {
                filteredItems.add(item);
            }
        }
        notifyDataSetChanged();
    }

    public Set<String> getSelectedIds() {
        return new HashSet<>(selectedIds);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ItemForwardTargetBinding b = ItemForwardTargetBinding.inflate(inflater, parent, false);
        return new ViewHolder(b);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TargetItem item = filteredItems.get(position);
        holder.bind(item, selectedIds.contains(item.channelId));
    }

    @Override
    public int getItemCount() {
        return filteredItems.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemForwardTargetBinding b;

        ViewHolder(ItemForwardTargetBinding binding) {
            super(binding.getRoot());
            this.b = binding;
        }

        void bind(TargetItem item, boolean checked) {
            b.tvTitle.setText(item.title);
            b.tvAvatarFallback.setText(firstLetter(item.title));
            if (TextUtils.isEmpty(item.subtitle)) {
                b.tvSubtitle.setVisibility(View.GONE);
            } else {
                b.tvSubtitle.setVisibility(View.VISIBLE);
                b.tvSubtitle.setText(item.subtitle);
            }

            b.cbSelect.setOnCheckedChangeListener(null);
            b.cbSelect.setChecked(checked);

            View.OnClickListener toggle = v -> {
                boolean nowChecked = !selectedIds.contains(item.channelId);
                if (nowChecked) {
                    selectedIds.add(item.channelId);
                } else {
                    selectedIds.remove(item.channelId);
                }
                b.cbSelect.setChecked(nowChecked);
                if (selectionChangedListener != null) {
                    selectionChangedListener.onSelectionChanged(getSelectedIds());
                }
            };

            b.getRoot().setOnClickListener(toggle);
            b.cbSelect.setOnClickListener(toggle);
        }

        private String firstLetter(String title) {
            if (title == null || title.trim().isEmpty()) return "?";
            return title.trim().substring(0, 1).toUpperCase(Locale.getDefault());
        }
    }
}
