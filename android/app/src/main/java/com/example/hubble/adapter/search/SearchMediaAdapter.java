package com.example.hubble.adapter.search;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.hubble.data.api.NetworkConfig;
import com.example.hubble.data.model.search.SearchAttachmentDto;
import com.example.hubble.databinding.ItemSearchMediaBinding;

import java.util.ArrayList;
import java.util.List;

public class SearchMediaAdapter
        extends RecyclerView.Adapter<SearchMediaAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onMediaClick(SearchAttachmentDto item);
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
        ItemSearchMediaBinding binding = ItemSearchMediaBinding.inflate(
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
        private final ItemSearchMediaBinding b;

        ViewHolder(ItemSearchMediaBinding b) {
            super(b.getRoot());
            this.b = b;
        }

        void bind(SearchAttachmentDto item) {
            String url = item.getUrl() != null ? NetworkConfig.resolveUrl(item.getUrl()) : null;
            Glide.with(b.ivThumbnail.getContext())
                    .load(url)
                    .centerCrop()
                    .into(b.ivThumbnail);
            b.getRoot().setOnClickListener(v -> {
                if (listener != null) listener.onMediaClick(item);
            });
        }
    }
}
