package com.example.hubble.adapter.server;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.hubble.R;
import com.example.hubble.data.model.server.RoleResponse;
import com.example.hubble.data.model.server.ServerMemberResponse;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ChannelAccessAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ROLE = 1;
    private static final int TYPE_MEMBER = 2;

    private final List<Object> items = new ArrayList<>();
    private final Set<String> selectedRoleIds = new HashSet<>();
    private final Set<String> selectedMemberIds = new HashSet<>();
    private OnSelectionChangedListener listener;

    public interface OnSelectionChangedListener {
        void onSelectionChanged(Set<String> roleIds, Set<String> memberIds);
    }

    public void setOnSelectionChangedListener(OnSelectionChangedListener listener) {
        this.listener = listener;
    }

    public void setData(List<RoleResponse> roles, List<ServerMemberResponse> members) {
        items.clear();
        selectedRoleIds.clear();
        selectedMemberIds.clear();

        if (roles != null && !roles.isEmpty()) {
            items.add(new SectionHeader(R.string.create_channel_section_roles));
            items.addAll(roles);
        }
        if (members != null && !members.isEmpty()) {
            items.add(new SectionHeader(R.string.create_channel_section_members));
            items.addAll(members);
        }
        notifyDataSetChanged();
    }

    public void filter(String query, List<RoleResponse> allRoles, List<ServerMemberResponse> allMembers) {
        items.clear();
        String q = query == null ? "" : query.toLowerCase().trim();

        List<RoleResponse> filteredRoles = new ArrayList<>();
        for (RoleResponse r : allRoles) {
            if (q.isEmpty() || (r.getName() != null && r.getName().toLowerCase().contains(q))) {
                filteredRoles.add(r);
            }
        }

        List<ServerMemberResponse> filteredMembers = new ArrayList<>();
        for (ServerMemberResponse m : allMembers) {
            String name = m.getDisplayName() != null ? m.getDisplayName() : m.getUsername();
            if (q.isEmpty() || (name != null && name.toLowerCase().contains(q))) {
                filteredMembers.add(m);
            }
        }

        if (!filteredRoles.isEmpty()) {
            items.add(new SectionHeader(R.string.create_channel_section_roles));
            items.addAll(filteredRoles);
        }
        if (!filteredMembers.isEmpty()) {
            items.add(new SectionHeader(R.string.create_channel_section_members));
            items.addAll(filteredMembers);
        }
        notifyDataSetChanged();
    }

    public Set<String> getSelectedRoleIds() { return selectedRoleIds; }
    public Set<String> getSelectedMemberIds() { return selectedMemberIds; }

    public boolean hasSelections() {
        return !selectedRoleIds.isEmpty() || !selectedMemberIds.isEmpty();
    }

    @Override
    public int getItemViewType(int position) {
        Object item = items.get(position);
        if (item instanceof SectionHeader) return TYPE_HEADER;
        if (item instanceof RoleResponse) return TYPE_ROLE;
        return TYPE_MEMBER;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_HEADER) {
            return new HeaderVH(inflater.inflate(R.layout.item_access_section_header, parent, false));
        } else if (viewType == TYPE_ROLE) {
            return new RoleVH(inflater.inflate(R.layout.item_access_role, parent, false));
        } else {
            return new MemberVH(inflater.inflate(R.layout.item_access_member, parent, false));
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Object item = items.get(position);
        if (holder instanceof HeaderVH) {
            ((HeaderVH) holder).bind((SectionHeader) item);
        } else if (holder instanceof RoleVH) {
            ((RoleVH) holder).bind((RoleResponse) item);
        } else if (holder instanceof MemberVH) {
            ((MemberVH) holder).bind((ServerMemberResponse) item);
        }
    }

    @Override
    public int getItemCount() { return items.size(); }

    // ── ViewHolders ───────────────────────────────────────────────────────

    static class HeaderVH extends RecyclerView.ViewHolder {
        final TextView tvHeader;
        HeaderVH(View v) {
            super(v);
            tvHeader = v.findViewById(R.id.tvSectionHeader);
        }
        void bind(SectionHeader header) {
            tvHeader.setText(header.stringResId);
        }
    }

    class RoleVH extends RecyclerView.ViewHolder {
        final TextView tvName;
        final CheckBox cb;
        RoleVH(View v) {
            super(v);
            tvName = v.findViewById(R.id.tvRoleName);
            cb = v.findViewById(R.id.cbSelected);
            v.setOnClickListener(x -> cb.performClick());
            cb.setOnCheckedChangeListener((btn, checked) -> {
                int pos = getBindingAdapterPosition();
                if (pos == RecyclerView.NO_POSITION || pos >= items.size()) return;
                RoleResponse role = (RoleResponse) items.get(pos);
                if (checked) selectedRoleIds.add(role.getId());
                else selectedRoleIds.remove(role.getId());
                if (listener != null) listener.onSelectionChanged(selectedRoleIds, selectedMemberIds);
            });
        }
        void bind(RoleResponse role) {
            tvName.setText(role.getName());
            cb.setOnCheckedChangeListener(null);
            cb.setChecked(selectedRoleIds.contains(role.getId()));
            cb.setOnCheckedChangeListener((btn, checked) -> {
                if (checked) selectedRoleIds.add(role.getId());
                else selectedRoleIds.remove(role.getId());
                if (listener != null) listener.onSelectionChanged(selectedRoleIds, selectedMemberIds);
            });
        }
    }

    class MemberVH extends RecyclerView.ViewHolder {
        final ImageView ivAvatar;
        final TextView tvInitial, tvDisplayName, tvUsername;
        final CheckBox cb;
        MemberVH(View v) {
            super(v);
            ivAvatar = v.findViewById(R.id.ivAvatar);
            tvInitial = v.findViewById(R.id.tvAvatarInitial);
            tvDisplayName = v.findViewById(R.id.tvDisplayName);
            tvUsername = v.findViewById(R.id.tvUsername);
            cb = v.findViewById(R.id.cbSelected);
            v.setOnClickListener(x -> cb.performClick());
            cb.setOnCheckedChangeListener((btn, checked) -> {
                int pos = getBindingAdapterPosition();
                if (pos == RecyclerView.NO_POSITION || pos >= items.size()) return;
                ServerMemberResponse m = (ServerMemberResponse) items.get(pos);
                if (checked) selectedMemberIds.add(m.getUserId());
                else selectedMemberIds.remove(m.getUserId());
                if (listener != null) listener.onSelectionChanged(selectedRoleIds, selectedMemberIds);
            });
        }
        void bind(ServerMemberResponse member) {
            String name = member.getDisplayName() != null ? member.getDisplayName() : member.getUsername();
            tvDisplayName.setText(name);
            tvUsername.setText(member.getUsername());

            if (member.getAvatarUrl() != null && !member.getAvatarUrl().isEmpty()) {
                ivAvatar.setVisibility(View.VISIBLE);
                tvInitial.setVisibility(View.GONE);
                Glide.with(ivAvatar.getContext())
                        .load(member.getAvatarUrl())
                        .circleCrop()
                        .into(ivAvatar);
            } else {
                ivAvatar.setVisibility(View.GONE);
                tvInitial.setVisibility(View.VISIBLE);
                tvInitial.setText(name != null && !name.isEmpty()
                        ? name.substring(0, 1).toUpperCase() : "?");
            }

            cb.setOnCheckedChangeListener(null);
            cb.setChecked(selectedMemberIds.contains(member.getUserId()));
            cb.setOnCheckedChangeListener((btn, checked) -> {
                if (checked) selectedMemberIds.add(member.getUserId());
                else selectedMemberIds.remove(member.getUserId());
                if (listener != null) listener.onSelectionChanged(selectedRoleIds, selectedMemberIds);
            });
        }
    }

    // ── Section header model ──────────────────────────────────────────────
    static class SectionHeader {
        final int stringResId;
        SectionHeader(int stringResId) { this.stringResId = stringResId; }
    }
}
