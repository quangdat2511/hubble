package com.example.hubble.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hubble.data.model.NewMessageItem;
import com.example.hubble.databinding.ItemNewMessageFriendBinding;
import com.example.hubble.databinding.ItemNewMessageSectionBinding;

import java.util.ArrayList;
import java.util.List;

public class NewMessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public interface OnFriendClickListener {
        void onFriendClick(NewMessageItem.Friend friend);
    }

    private final List<NewMessageItem> items = new ArrayList<>();
    private OnFriendClickListener listener;

    public void setItems(List<NewMessageItem> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    public void setOnFriendClickListener(OnFriendClickListener listener) {
        this.listener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position).getType();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == NewMessageItem.TYPE_SECTION) {
            ItemNewMessageSectionBinding binding = ItemNewMessageSectionBinding.inflate(inflater, parent, false);
            return new SectionViewHolder(binding);
        }

        ItemNewMessageFriendBinding binding = ItemNewMessageFriendBinding.inflate(inflater, parent, false);
        return new FriendViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        NewMessageItem item = items.get(position);
        if (holder instanceof SectionViewHolder && item instanceof NewMessageItem.Section) {
            SectionViewHolder sectionViewHolder = (SectionViewHolder) holder;
            NewMessageItem.Section section = (NewMessageItem.Section) item;
            sectionViewHolder.bind(section);
        } else if (holder instanceof FriendViewHolder && item instanceof NewMessageItem.Friend) {
            FriendViewHolder friendViewHolder = (FriendViewHolder) holder;
            NewMessageItem.Friend friend = (NewMessageItem.Friend) item;
            friendViewHolder.bind(friend);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class SectionViewHolder extends RecyclerView.ViewHolder {
        private final ItemNewMessageSectionBinding binding;

        SectionViewHolder(ItemNewMessageSectionBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(NewMessageItem.Section section) {
            binding.tvSection.setText(section.getTitle());
        }
    }

    class FriendViewHolder extends RecyclerView.ViewHolder {
        private final ItemNewMessageFriendBinding binding;

        FriendViewHolder(ItemNewMessageFriendBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(NewMessageItem.Friend friend) {
            binding.tvDisplayName.setText(friend.getDisplayName());
            binding.tvUsername.setText(friend.getUsername());
            binding.tvInitial.setText(friend.getDisplayName().isEmpty()
                    ? "?"
                    : friend.getDisplayName().substring(0, 1).toUpperCase());
            binding.viewPresence.setVisibility(friend.isOnline() ? View.VISIBLE : View.GONE);

            if (friend.getBadge() == null || friend.getBadge().trim().isEmpty()) {
                binding.chipBadge.setVisibility(View.GONE);
            } else {
                binding.chipBadge.setVisibility(View.VISIBLE);
                binding.chipBadge.setText(friend.getBadge());
            }

            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) {
                    listener.onFriendClick(friend);
                }
            });
        }
    }
}
