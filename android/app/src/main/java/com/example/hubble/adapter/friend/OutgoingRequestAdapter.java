package com.example.hubble.adapter.friend;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.hubble.R;
import com.example.hubble.data.model.dm.FriendRequestResponse;
import com.example.hubble.databinding.ItemOutgoingRequestBinding;
import java.util.ArrayList;
import java.util.List;

public class OutgoingRequestAdapter extends RecyclerView.Adapter<OutgoingRequestAdapter.ViewHolder> {

    private final List<FriendRequestResponse> requests = new ArrayList<>();
    private final OnCancelRequestListener listener;

    public interface OnCancelRequestListener {
        void onCancel(FriendRequestResponse request);
    }

    public OutgoingRequestAdapter(OnCancelRequestListener listener) {
        this.listener = listener;
    }

    public void setRequests(List<FriendRequestResponse> newRequests) {
        requests.clear();
        if (newRequests != null) {
            requests.addAll(newRequests);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemOutgoingRequestBinding binding = ItemOutgoingRequestBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FriendRequestResponse request = requests.get(position);

        if (request.getUser() != null) {
            holder.binding.tvDisplayName.setText(request.getUser().getDisplayName());
            holder.binding.tvUsername.setText(request.getUser().getUsername());

            Glide.with(holder.itemView.getContext())
                    .load(request.getUser().getAvatarUrl())
                    .placeholder(R.drawable.ic_person)
                    .circleCrop()
                    .into(holder.binding.ivAvatar);
        }

        holder.binding.btnCancel.setOnClickListener(v -> {
            if (listener != null) {
                listener.onCancel(request);
            }
        });
    }

    @Override
    public int getItemCount() {
        return requests.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        final ItemOutgoingRequestBinding binding;

        public ViewHolder(ItemOutgoingRequestBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}