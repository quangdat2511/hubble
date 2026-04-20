package com.example.hubble.adapter.home;

import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.hubble.data.api.NetworkConfig;
import com.example.hubble.data.model.dm.DmConversationItem;
import com.example.hubble.data.model.server.ServerItem;
import com.example.hubble.databinding.ItemServerBinding;
import com.example.hubble.databinding.ItemServerSeparatorBinding;
import com.example.hubble.utils.AvatarPlaceholderUtils;
import com.google.android.material.shape.RelativeCornerSize;
import com.google.android.material.shape.ShapeAppearanceModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ServerSidebarAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public interface OnServerClickListener {
        void onServerClick(ServerItem server, int position);
    }

    public interface OnDmClickListener {
        void onDmClick(DmConversationItem item);
    }

    private final List<ServerItem> servers = new ArrayList<>();
    private final List<DmConversationItem> dmItems = new ArrayList<>();
    private final Map<String, Integer> unreadByServerId = new HashMap<>();
    private final Map<String, Integer> mentionsByServerId = new HashMap<>();
    private int selectedPosition = 0;
    private OnServerClickListener listener;
    private OnDmClickListener dmClickListener;

    public void setServers(List<ServerItem> items) {
        List<ServerItem> newItems = items != null ? new ArrayList<>(items) : new ArrayList<>();
        servers.clear();
        servers.addAll(newItems);
        if (servers.isEmpty()) {
            selectedPosition = 0;
        } else if (selectedPosition >= servers.size()) {
            selectedPosition = 0;
        }
        notifyDataSetChanged();
    }

    public void setOnServerClickListener(OnServerClickListener l) {
        this.listener = l;
    }

    public void setOnDmClickListener(OnDmClickListener listener) {
        this.dmClickListener = listener;
    }

    public void setDmItems(List<DmConversationItem> items) {
        dmItems.clear();
        if (items != null) {
            dmItems.addAll(items);
        }
        notifyDataSetChanged();
    }

    public void setUnreadByServerId(Map<String, Integer> map) {
        unreadByServerId.clear();
        if (map != null) {
            unreadByServerId.putAll(map);
        }
        notifyDataSetChanged();
    }

    public void setMentionsByServerId(Map<String, Integer> map) {
        mentionsByServerId.clear();
        if (map != null) {
            mentionsByServerId.putAll(map);
        }
        notifyDataSetChanged();
    }

    public void setSelectedPosition(int position) {
        int old = selectedPosition;
        if (old == position) return;
        selectedPosition = position;
        if (old >= 0 && old < servers.size()) notifyItemChanged(serverIndexToAdapterPosition(old));
        if (position >= 0 && position < servers.size()) notifyItemChanged(serverIndexToAdapterPosition(position));
    }

    @Override
    public int getItemViewType(int position) {
        return isSeparatorPosition(position) ? SidebarEntry.TYPE_SEPARATOR : SidebarEntry.TYPE_ITEM;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == SidebarEntry.TYPE_SEPARATOR) {
            ItemServerSeparatorBinding separatorBinding = ItemServerSeparatorBinding.inflate(
                    LayoutInflater.from(parent.getContext()), parent, false);
            return new SeparatorViewHolder(separatorBinding);
        }
        ItemServerBinding b = ItemServerBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        ServerItemViewHolder holder = new ServerItemViewHolder(b);

        holder.itemView.setOnClickListener(v -> {
            int pos = holder.getBindingAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) return;

            SidebarEntry entry = getEntryAt(pos);
            if (entry == null) {
                return;
            }

            if (entry.type == SidebarEntry.TYPE_DM) {
                if (dmClickListener != null && entry.dmItem != null) {
                    dmClickListener.onDmClick(entry.dmItem);
                }
                return;
            }

            int serverIndex = adapterPositionToServerIndex(pos);
            if (serverIndex != selectedPosition) {
                int old = selectedPosition;
                selectedPosition = serverIndex;
                int oldAdapterPosition = old >= 0 ? serverIndexToAdapterPosition(old) : -1;
                int newAdapterPosition = selectedPosition >= 0 ? serverIndexToAdapterPosition(selectedPosition) : -1;
                if (oldAdapterPosition >= 0 && oldAdapterPosition < getItemCount()) {
                    notifyItemChanged(oldAdapterPosition);
                }
                if (newAdapterPosition >= 0 && newAdapterPosition < getItemCount()) {
                    notifyItemChanged(newAdapterPosition);
                }
            }
            if (listener != null && entry.serverItem != null) {
                listener.onServerClick(entry.serverItem, serverIndex);
            }
        });

        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (getItemViewType(position) == SidebarEntry.TYPE_SEPARATOR) {
            return;
        }
        SidebarEntry entry = getEntryAt(position);
        if (entry == null) {
            return;
        }
        ServerItemViewHolder itemHolder = (ServerItemViewHolder) holder;
        if (entry.type == SidebarEntry.TYPE_DM) {
            itemHolder.bindDm(entry.dmItem);
            return;
        }
        int serverIndex = adapterPositionToServerIndex(position);
        boolean selected = (serverIndex == selectedPosition);
        boolean hasUnread = false;
        int mentions = 0;
        if (entry.serverItem != null && entry.serverItem.getId() != null) {
            Integer u = unreadByServerId.get(entry.serverItem.getId());
            hasUnread = u != null && u > 0;
            Integer m = mentionsByServerId.get(entry.serverItem.getId());
            mentions = m != null ? m : 0;
        }
        itemHolder.bindServer(entry.serverItem, selected, hasUnread, mentions);
    }

    @Override
    public int getItemCount() {
        return dmItems.size() + servers.size() + (shouldShowSeparator() ? 1 : 0);
    }

    @Nullable
    private SidebarEntry getEntryAt(int position) {
        if (position < 0 || position >= getItemCount()) {
            return null;
        }
        if (isSeparatorPosition(position)) {
            return null;
        }
        if (position < dmItems.size()) {
            return SidebarEntry.dm(dmItems.get(position));
        }
        int serverIndex = adapterPositionToServerIndex(position);
        return SidebarEntry.server(servers.get(serverIndex));
    }

    private boolean shouldShowSeparator() {
        return !servers.isEmpty();
    }

    private boolean isSeparatorPosition(int adapterPosition) {
        return shouldShowSeparator() && adapterPosition == dmItems.size();
    }

    private int adapterPositionToServerIndex(int adapterPosition) {
        return adapterPosition - dmItems.size() - (shouldShowSeparator() ? 1 : 0);
    }

    private int serverIndexToAdapterPosition(int serverIndex) {
        return dmItems.size() + (shouldShowSeparator() ? 1 : 0) + serverIndex;
    }

    public static class ServerItemViewHolder extends RecyclerView.ViewHolder {
        private final ItemServerBinding b;

        ServerItemViewHolder(ItemServerBinding binding) {
            super(binding.getRoot());
            this.b = binding;
        }

        void bindServer(@Nullable ServerItem server, boolean isSelected,
                        boolean hasUnread, int mentionCount) {
            if (server == null) {
                return;
            }
            // Selected: tall rounded pill on far-left edge.
            b.viewActiveIndicator.setVisibility(isSelected ? View.VISIBLE : View.GONE);

            // Unread (not selected): small dot on far-left edge.
            // Discord rule: when the server is active we hide the unread dot.
            boolean showUnreadDot = hasUnread && !isSelected;
            b.viewUnreadPill.setVisibility(showUnreadDot ? View.VISIBLE : View.GONE);

            // Red circular badge: ONLY for mentions (pings). Plain unread shows no number.
            if (mentionCount > 0) {
                b.tvServerUnreadBadge.setVisibility(View.VISIBLE);
                b.tvServerUnreadBadge.setText(
                        mentionCount > 99 ? "99+" : String.valueOf(mentionCount));
            } else {
                b.tvServerUnreadBadge.setVisibility(View.GONE);
            }

            float density = b.ivServerIcon.getContext().getResources().getDisplayMetrics().density;
            ShapeAppearanceModel shapeModel = isSelected
                    ? ShapeAppearanceModel.builder().setAllCornerSizes(16f * density).build()
                    : ShapeAppearanceModel.builder().setAllCornerSizes(new RelativeCornerSize(0.5f)).build();
            b.ivServerIcon.setShapeAppearanceModel(shapeModel);

            if (server.getIconUrl() != null && !server.getIconUrl().isEmpty()) {
                b.tvServerInitials.setVisibility(View.GONE);
                Glide.with(b.ivServerIcon.getContext())
                        .load(server.getIconUrl())
                        .centerCrop()
                        .into(b.ivServerIcon);
            } else {
                Glide.with(b.ivServerIcon.getContext()).clear(b.ivServerIcon);
                b.ivServerIcon.setImageDrawable(new ColorDrawable(server.getBackgroundColor()));
                b.tvServerInitials.setVisibility(View.VISIBLE);
                b.tvServerInitials.setText(server.getInitials());
            }
        }

        void bindDm(@Nullable DmConversationItem dm) {
            if (dm == null) {
                return;
            }
            b.viewActiveIndicator.setVisibility(View.GONE);
            b.viewUnreadPill.setVisibility(View.GONE);

            int unread = Math.max(0, dm.getUnreadCount());
            if (unread > 0) {
                b.tvServerUnreadBadge.setVisibility(View.VISIBLE);
                b.tvServerUnreadBadge.setText(unread > 99 ? "99+" : String.valueOf(unread));
            } else {
                b.tvServerUnreadBadge.setVisibility(View.GONE);
            }

            ShapeAppearanceModel shapeModel = ShapeAppearanceModel.builder()
                    .setAllCornerSizes(new RelativeCornerSize(0.5f))
                    .build();
            b.ivServerIcon.setShapeAppearanceModel(shapeModel);

            String avatarUrl = NetworkConfig.resolveUrl(dm.getAvatarUrl());
            boolean hasAvatar = avatarUrl != null && !avatarUrl.trim().isEmpty();
            int avatarSize = b.ivServerIcon.getLayoutParams() != null
                    ? b.ivServerIcon.getLayoutParams().width
                    : b.ivServerIcon.getWidth();
            Drawable avatarFallback = AvatarPlaceholderUtils.createAvatarDrawable(
                    b.ivServerIcon.getContext(),
                    dm.getDisplayName(),
                    avatarSize
            );
            b.tvServerInitials.setVisibility(View.GONE);
            Glide.with(b.ivServerIcon.getContext()).clear(b.ivServerIcon);
            if (!hasAvatar) {
                b.ivServerIcon.setImageDrawable(avatarFallback);
            } else {
                b.ivServerIcon.setImageDrawable(null);
                Glide.with(b.ivServerIcon.getContext())
                        .load(avatarUrl)
                        .error(avatarFallback)
                        .fallback(avatarFallback)
                        .centerCrop()
                        .into(b.ivServerIcon);
            }
        }
    }

    public static class SeparatorViewHolder extends RecyclerView.ViewHolder {
        SeparatorViewHolder(ItemServerSeparatorBinding binding) {
            super(binding.getRoot());
        }
    }

    private static class SidebarEntry {
        static final int TYPE_ITEM = 0;
        static final int TYPE_SEPARATOR = 1;
        static final int TYPE_DM = 2;
        static final int TYPE_SERVER = 3;

        final int type;
        final DmConversationItem dmItem;
        final ServerItem serverItem;

        private SidebarEntry(int type, @Nullable DmConversationItem dmItem, @Nullable ServerItem serverItem) {
            this.type = type;
            this.dmItem = dmItem;
            this.serverItem = serverItem;
        }

        static SidebarEntry dm(DmConversationItem item) {
            return new SidebarEntry(TYPE_DM, item, null);
        }

        static SidebarEntry server(ServerItem item) {
            return new SidebarEntry(TYPE_SERVER, null, item);
        }
    }
}
