package com.example.hubble.adapter.search;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hubble.data.model.search.SearchAttachmentDto;
import com.example.hubble.databinding.ItemSearchFileBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SearchFileAdapter
        extends RecyclerView.Adapter<SearchFileAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onFileClick(SearchAttachmentDto item);
    }

    private final List<SearchAttachmentDto> items = new ArrayList<>();
    @Nullable private OnItemClickListener listener;

    public void setItems(List<SearchAttachmentDto> newItems) {
        items.clear();
        if (newItems != null) items.addAll(newItems);
        notifyDataSetChanged();
    }

    public void setOnItemClickListener(@Nullable OnItemClickListener l) {
        this.listener = l;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemSearchFileBinding binding = ItemSearchFileBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() { return items.size(); }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemSearchFileBinding b;

        ViewHolder(ItemSearchFileBinding b) {
            super(b.getRoot());
            this.b = b;
        }

        void bind(SearchAttachmentDto item) {
            b.tvFilename.setText(item.getFilename() != null ? item.getFilename() : "");
            b.tvFileSize.setText(formatSize(item.getSizeBytes()));
            b.getRoot().setOnClickListener(v -> {
                if (listener != null) listener.onFileClick(item);
            });
        }

        private String formatSize(Long bytes) {
            if (bytes == null || bytes <= 0) return "";
            if (bytes < 1024) return bytes + " B";
            if (bytes < 1024 * 1024) return String.format(Locale.getDefault(), "%.1f KB", bytes / 1024.0);
            return String.format(Locale.getDefault(), "%.1f MB", bytes / (1024.0 * 1024));
        }
    }
}
