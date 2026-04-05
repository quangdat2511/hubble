package com.example.hubble.adapter.settings;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.hubble.R;
import com.example.hubble.data.model.auth.SessionDto;
import com.example.hubble.databinding.ItemSessionBinding;
import java.util.ArrayList;
import java.util.List;

public class SessionAdapter extends RecyclerView.Adapter<SessionAdapter.ViewHolder> {

    private final List<SessionDto> sessions = new ArrayList<>();
    private final OnRevokeClickListener listener;

    public interface OnRevokeClickListener {
        void onRevokeClick(SessionDto session);
    }

    public SessionAdapter(OnRevokeClickListener listener) {
        this.listener = listener;
    }

    public void setSessions(List<SessionDto> newSessions) {
        sessions.clear();
        if (newSessions != null) {
            sessions.addAll(newSessions);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemSessionBinding binding = ItemSessionBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(sessions.get(position));
    }

    @Override
    public int getItemCount() {
        return sessions.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemSessionBinding binding;

        ViewHolder(ItemSessionBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(SessionDto session) {
            binding.tvDeviceName.setText(session.getDeviceName() != null
                    ? session.getDeviceName()
                    : binding.getRoot().getContext().getString(R.string.session_unknown_device));
            binding.tvIpAddress.setText(session.getIpAddress() != null
                    ? session.getIpAddress()
                    : binding.getRoot().getContext().getString(R.string.session_unknown_value));
            binding.tvLastActive.setText(session.getLastActiveAt());

            binding.btnRevoke.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onRevokeClick(session);
                }
            });
        }
    }
}
