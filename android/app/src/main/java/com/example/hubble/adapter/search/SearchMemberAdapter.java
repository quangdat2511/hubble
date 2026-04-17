package com.example.hubble.adapter.search;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.hubble.data.api.NetworkConfig;
import com.example.hubble.data.model.search.SearchMemberDto;
import com.example.hubble.databinding.ItemSearchMemberBinding;
import com.example.hubble.utils.AvatarPlaceholderUtils;

import java.util.ArrayList;
import java.util.List;

public class SearchMemberAdapter
        extends RecyclerView.Adapter<SearchMemberAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onMemberClick(SearchMemberDto item);
    }

    private final List<SearchMemberDto> items = new ArrayList<>();
    @Nullable private OnItemClickListener listener;

    public void setItems(List<SearchMemberDto> newItems) {
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
        ItemSearchMemberBinding binding = ItemSearchMemberBinding.inflate(
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
        private final ItemSearchMemberBinding b;

        ViewHolder(ItemSearchMemberBinding b) {
            super(b.getRoot());
            this.b = b;
        }

        void bind(SearchMemberDto item) {
            String display = item.getDisplayName() != null
                    ? item.getDisplayName() : item.getUsername();
            b.tvDisplayName.setText(display);
            b.tvUsername.setText(item.getUsername() != null ? "@" + item.getUsername() : "");

            String status = item.getStatus();
            if ("ONLINE".equalsIgnoreCase(status)) {
                b.statusDot.setVisibility(View.VISIBLE);
                b.statusDot.getBackground().setTint(
                        b.getRoot().getContext().getColor(com.example.hubble.R.color.color_success));
            } else {
                b.statusDot.setVisibility(View.GONE);
            }

            String avatarUrl = item.getAvatarUrl() != null
                    ? NetworkConfig.resolveUrl(item.getAvatarUrl()) : null;
            android.graphics.drawable.Drawable fallback =
                    AvatarPlaceholderUtils.createAvatarDrawable(
                            b.ivAvatar.getContext(), display, 40);
            Glide.with(b.ivAvatar.getContext())
                    .load(avatarUrl)
                    .placeholder(fallback)
                    .error(fallback)
                    .circleCrop()
                    .into(b.ivAvatar);

            b.getRoot().setOnClickListener(v -> {
                if (listener != null) listener.onMemberClick(item);
            });
        }
    }
}
