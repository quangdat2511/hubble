package com.example.hubble.adapter.dm;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.hubble.R;
import com.example.hubble.data.api.NetworkConfig;
import com.example.hubble.data.model.search.SearchMemberDto;
import com.example.hubble.databinding.ItemMentionSuggestionBinding;
import com.example.hubble.utils.AvatarPlaceholderUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView adapter for the mention autocomplete dropdown.
 * Row 0 is always the synthetic "@everyone" entry; subsequent rows are members.
 */
public class MentionDropdownAdapter
        extends RecyclerView.Adapter<MentionDropdownAdapter.ViewHolder> {

    private static final int VIEW_TYPE_EVERYONE = 0;
    private static final int VIEW_TYPE_MEMBER   = 1;

    public interface OnMentionSelectedListener {
        /** Called when a real member row is clicked. */
        void onMemberSelected(SearchMemberDto member);
        /** Called when the @everyone row is clicked. */
        void onEveryoneSelected();
    }

    private final List<SearchMemberDto> members = new ArrayList<>();
    @Nullable
    private OnMentionSelectedListener listener;
    private boolean showEveryone = true;

    public void setShowEveryone(boolean showEveryone) {
        this.showEveryone = showEveryone;
        notifyDataSetChanged();
    }

    public void setListener(@Nullable OnMentionSelectedListener listener) {
        this.listener = listener;
    }

    /** Replace the member list (excludes @everyone — it is always row 0). */
    public void setMembers(List<SearchMemberDto> newMembers) {
        members.clear();
        if (newMembers != null) members.addAll(newMembers);
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return (showEveryone ? 1 : 0) + members.size();
    }

    @Override
    public int getItemViewType(int position) {
        if (showEveryone && position == 0) return VIEW_TYPE_EVERYONE;
        return VIEW_TYPE_MEMBER;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemMentionSuggestionBinding binding = ItemMentionSuggestionBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (showEveryone && position == 0) {
            holder.bindEveryone();
        } else {
            holder.bindMember(members.get(showEveryone ? position - 1 : position));
        }
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemMentionSuggestionBinding b;

        ViewHolder(@NonNull ItemMentionSuggestionBinding binding) {
            super(binding.getRoot());
            this.b = binding;
        }

        void bindEveryone() {
            b.tvDisplayName.setText(b.getRoot().getContext().getString(R.string.mention_everyone_label));
            b.tvUsername.setText("@everyone");
            Glide.with(b.ivAvatar.getContext()).clear(b.ivAvatar);
            b.ivAvatar.setImageResource(R.drawable.ic_at_sign);
            b.getRoot().setOnClickListener(v -> {
                if (listener != null) listener.onEveryoneSelected();
            });
        }

        void bindMember(@NonNull SearchMemberDto member) {
            String displayName = member.getDisplayName() != null
                    ? member.getDisplayName() : member.getUsername();
            String username = member.getUsername() != null ? "@" + member.getUsername() : "";
            b.tvDisplayName.setText(displayName);
            b.tvUsername.setText(username);

            String avatarUrl = member.getAvatarUrl() != null
                    ? NetworkConfig.resolveUrl(member.getAvatarUrl()) : null;
            android.graphics.drawable.Drawable fallback =
                    AvatarPlaceholderUtils.createAvatarDrawable(
                            b.ivAvatar.getContext(), displayName, 32);
            Glide.with(b.ivAvatar.getContext())
                    .load(avatarUrl)
                    .placeholder(fallback)
                    .error(fallback)
                    .circleCrop()
                    .into(b.ivAvatar);

            b.getRoot().setOnClickListener(v -> {
                if (listener != null) listener.onMemberSelected(member);
            });
        }
    }
}
