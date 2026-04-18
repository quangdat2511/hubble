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
import com.example.hubble.databinding.ItemMembersHeaderBinding;
import com.example.hubble.databinding.ItemSearchMemberBinding;
import com.example.hubble.utils.AvatarPlaceholderUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class SearchMemberAdapter
        extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_MEMBER = 1;

    public interface OnItemClickListener {
        void onMemberClick(SearchMemberDto item);
    }

    // ── Flat list item ────────────────────────────────────────────────────────

    public static class ListItem {
        final int type;
        @Nullable final String header;
        @Nullable final SearchMemberDto member;

        private ListItem(String header) {
            this.type   = TYPE_HEADER;
            this.header = header;
            this.member = null;
        }

        private ListItem(SearchMemberDto member) {
            this.type   = TYPE_MEMBER;
            this.header = null;
            this.member = member;
        }

        static ListItem ofHeader(String label) { return new ListItem(label); }
        static ListItem ofMember(SearchMemberDto m) { return new ListItem(m); }
    }

    // ── State ─────────────────────────────────────────────────────────────────

    private final List<ListItem> flatList = new ArrayList<>();
    @Nullable private OnItemClickListener listener;

    // ── Public API ────────────────────────────────────────────────────────────

    /** Plain list without clustering (used for DM friends). */
    public void setItems(List<SearchMemberDto> newItems) {
        flatList.clear();
        if (newItems != null) {
            for (SearchMemberDto m : newItems) {
                flatList.add(ListItem.ofMember(m));
            }
        }
        notifyDataSetChanged();
    }

    /**
     * Builds a clustered list: "Me" section at the top, then A–Z sections
     * sorted alphabetically by display name / username.
     */
    public void setItemsWithClustering(List<SearchMemberDto> members) {
        flatList.clear();
        if (members == null || members.isEmpty()) {
            notifyDataSetChanged();
            return;
        }

        List<SearchMemberDto> meList  = new ArrayList<>();
        List<SearchMemberDto> others  = new ArrayList<>();
        for (SearchMemberDto m : members) {
            if (m.isSelf()) meList.add(m); else others.add(m);
        }

        others.sort(Comparator.comparing(
                m -> (m.getDisplayName() != null ? m.getDisplayName() : m.getUsername()),
                String.CASE_INSENSITIVE_ORDER));

        if (!meList.isEmpty()) {
            flatList.add(ListItem.ofHeader("Me"));
            for (SearchMemberDto m : meList) flatList.add(ListItem.ofMember(m));
        }

        String currentLetter = null;
        for (SearchMemberDto m : others) {
            String name = m.getDisplayName() != null ? m.getDisplayName() : m.getUsername();
            String letter = (name != null && !name.isEmpty())
                    ? String.valueOf(name.charAt(0)).toUpperCase()
                    : "#";
            if (!letter.equals(currentLetter)) {
                currentLetter = letter;
                flatList.add(ListItem.ofHeader(letter));
            }
            flatList.add(ListItem.ofMember(m));
        }

        notifyDataSetChanged();
    }

    public void setOnItemClickListener(@Nullable OnItemClickListener l) {
        this.listener = l;
    }

    // ── RecyclerView.Adapter ──────────────────────────────────────────────────

    @Override
    public int getItemViewType(int position) {
        return flatList.get(position).type;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_HEADER) {
            ItemMembersHeaderBinding b =
                    ItemMembersHeaderBinding.inflate(inflater, parent, false);
            return new HeaderViewHolder(b);
        }
        ItemSearchMemberBinding b =
                ItemSearchMemberBinding.inflate(inflater, parent, false);
        return new MemberViewHolder(b);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ListItem item = flatList.get(position);
        if (holder instanceof HeaderViewHolder) {
            ((HeaderViewHolder) holder).bind(item.header);
        } else if (holder instanceof MemberViewHolder) {
            ((MemberViewHolder) holder).bind(item.member);
        }
    }

    @Override
    public int getItemCount() { return flatList.size(); }

    // ── ViewHolders ───────────────────────────────────────────────────────────

    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        private final ItemMembersHeaderBinding b;

        HeaderViewHolder(ItemMembersHeaderBinding b) {
            super(b.getRoot());
            this.b = b;
        }

        void bind(String label) {
            b.tvSectionLabel.setText(label);
        }
    }

    class MemberViewHolder extends RecyclerView.ViewHolder {
        private final ItemSearchMemberBinding b;

        MemberViewHolder(ItemSearchMemberBinding b) {
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
