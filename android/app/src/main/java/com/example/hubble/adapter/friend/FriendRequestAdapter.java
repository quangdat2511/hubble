package com.example.hubble.adapter.friend;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.hubble.data.model.dm.FriendRequestResponse;
import com.example.hubble.databinding.ItemFriendRequestBinding;
import java.util.ArrayList;
import java.util.List;

public class FriendRequestAdapter extends RecyclerView.Adapter<FriendRequestAdapter.ViewHolder> {
    private final List<FriendRequestResponse> requests = new ArrayList<>();
    private final OnRequestListener listener;

    public interface OnRequestListener {
        void onAccept(FriendRequestResponse request);
        void onDecline(FriendRequestResponse request);
    }

    public FriendRequestAdapter(OnRequestListener listener) {
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
        ItemFriendRequestBinding binding = ItemFriendRequestBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(requests.get(position));
    }

    @Override
    public int getItemCount() {
        return requests.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemFriendRequestBinding binding;

        ViewHolder(ItemFriendRequestBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(FriendRequestResponse request) {
            if (request.getUser() != null) {
                String displayName = (request.getUser().getDisplayName() != null && !request.getUser().getDisplayName().isEmpty())
                        ? request.getUser().getDisplayName() : request.getUser().getUsername();
                binding.tvDisplayName.setText(displayName);
                binding.tvUsername.setText(request.getUser().getUsername());
            }

            binding.btnAccept.setOnClickListener(v -> {
                if (listener != null) listener.onAccept(request);
            });
            binding.btnDecline.setOnClickListener(v -> {
                if (listener != null) listener.onDecline(request);
            });
        }
    }
}