package com.example.hubble.adapter.server;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hubble.data.model.server.MemberBriefResponse;
import com.example.hubble.databinding.ItemMemberSelectableBinding;

import java.util.List;

public class RoleMemberAdapter extends RecyclerView.Adapter<RoleMemberAdapter.ViewHolder> {

    public interface OnRemoveClickListener {
        void onRemove(MemberBriefResponse member);
    }

    private List<MemberBriefResponse> members;
    private OnRemoveClickListener removeListener;

    public RoleMemberAdapter(List<MemberBriefResponse> members) {
        this.members = members;
    }

    public void setOnRemoveClickListener(OnRemoveClickListener listener) {
        this.removeListener = listener;
    }

    public void updateList(List<MemberBriefResponse> newMembers) {
        this.members = newMembers;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemMemberSelectableBinding binding = ItemMemberSelectableBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(members.get(position));
    }

    @Override
    public int getItemCount() {
        return members.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemMemberSelectableBinding binding;

        ViewHolder(ItemMemberSelectableBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(MemberBriefResponse member) {
            String display = member.getDisplayName() != null ? member.getDisplayName() : member.getUsername();
            binding.tvDisplayName.setText(display);
            binding.tvUsername.setText(member.getUsername());

            binding.checkBox.setVisibility(View.GONE);
            binding.ivRemove.setVisibility(View.VISIBLE);
            binding.ivRemove.setOnClickListener(v -> {
                if (removeListener != null) removeListener.onRemove(member);
            });
        }
    }
}
