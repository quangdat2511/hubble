package com.example.hubble.adapter.dm;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hubble.data.model.dm.DmMessageItem;
import com.example.hubble.databinding.ItemDmMessageMeBinding;
import com.example.hubble.databinding.ItemDmMessageOtherBinding;

import java.util.ArrayList;
import java.util.List;

public class DmMessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_ME = 1;
    private static final int TYPE_OTHER = 2;

    private final List<DmMessageItem> items = new ArrayList<>();

    public void setItems(List<DmMessageItem> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    public void appendItem(DmMessageItem item) {
        if (item == null) return;
        items.add(item);
        notifyItemInserted(items.size() - 1);
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position).isMine() ? TYPE_ME : TYPE_OTHER;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_ME) {
            return new MeHolder(ItemDmMessageMeBinding.inflate(inflater, parent, false));
        }
        return new OtherHolder(ItemDmMessageOtherBinding.inflate(inflater, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        DmMessageItem item = items.get(position);
        if (holder instanceof MeHolder) {
            ((MeHolder) holder).bind(item);
        } else {
            ((OtherHolder) holder).bind(item);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class MeHolder extends RecyclerView.ViewHolder {
        private final ItemDmMessageMeBinding binding;

        MeHolder(ItemDmMessageMeBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(DmMessageItem item) {
            binding.tvMessage.setText(item.getContent());
            binding.tvTime.setText(item.getTimestamp());
        }
    }

    static class OtherHolder extends RecyclerView.ViewHolder {
        private final ItemDmMessageOtherBinding binding;

        OtherHolder(ItemDmMessageOtherBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(DmMessageItem item) {
            binding.tvName.setText(item.getSenderName());
            binding.tvMessage.setText(item.getContent());
            binding.tvTime.setText(item.getTimestamp());
        }
    }
}


