package com.example.hubble.adapter.search;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hubble.data.model.search.SearchChannelDto;
import com.example.hubble.databinding.ItemSearchChannelBinding;

import java.util.ArrayList;
import java.util.List;

public class SearchChannelAdapter
        extends RecyclerView.Adapter<SearchChannelAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onChannelClick(SearchChannelDto item);
    }

    private final List<SearchChannelDto> items = new ArrayList<>();
    @Nullable private OnItemClickListener listener;

    public void setItems(List<SearchChannelDto> newItems) {
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
        ItemSearchChannelBinding binding = ItemSearchChannelBinding.inflate(
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
        private final ItemSearchChannelBinding b;

        ViewHolder(ItemSearchChannelBinding b) {
            super(b.getRoot());
            this.b = b;
        }

        void bind(SearchChannelDto item) {
            b.tvChannelName.setText(item.getName() != null ? item.getName() : "");
            if (item.getTopic() != null && !item.getTopic().isEmpty()) {
                b.tvChannelTopic.setText(item.getTopic());
                b.tvChannelTopic.setVisibility(View.VISIBLE);
            } else {
                b.tvChannelTopic.setVisibility(View.GONE);
            }
            b.getRoot().setOnClickListener(v -> {
                if (listener != null) listener.onChannelClick(item);
            });
        }
    }
}
