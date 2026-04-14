package com.example.hubble.adapter.voice;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.hubble.R;
import com.example.hubble.data.api.NetworkConfig;
import com.example.hubble.data.model.voice.VoiceParticipant;
import com.example.hubble.databinding.ItemVoiceParticipantBinding;
import com.example.hubble.utils.AvatarPlaceholderUtils;

import java.util.ArrayList;
import java.util.List;

public class VoiceParticipantAdapter extends RecyclerView.Adapter<VoiceParticipantAdapter.ViewHolder> {

    private final List<VoiceParticipant> participants = new ArrayList<>();
    private boolean compactMode = false;

    public void setCompactMode(boolean compact) {
        if (this.compactMode != compact) {
            this.compactMode = compact;
            notifyDataSetChanged();
        }
    }

    public void submitList(List<VoiceParticipant> newList) {
        participants.clear();
        if (newList != null) {
            participants.addAll(newList);
        }
        notifyDataSetChanged();
    }

    public void updateSpeaking(String userId, boolean speaking) {
        for (int i = 0; i < participants.size(); i++) {
            if (participants.get(i).getUserId().equals(userId)) {
                participants.get(i).setSpeaking(speaking);
                notifyItemChanged(i);
                break;
            }
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemVoiceParticipantBinding binding = ItemVoiceParticipantBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(participants.get(position), compactMode);
    }

    @Override
    public int getItemCount() {
        return participants.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemVoiceParticipantBinding binding;

        ViewHolder(ItemVoiceParticipantBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(VoiceParticipant participant, boolean compact) {
            Context context = binding.getRoot().getContext();
            binding.tvDisplayName.setText(participant.getDisplayName());
            binding.tvDisplayName.setVisibility(compact ? View.GONE : View.VISIBLE);

            // Speaking indicator with pulse animation
            if (participant.isSpeaking()) {
                binding.viewSpeakingIndicator.setVisibility(View.VISIBLE);
                if (binding.viewSpeakingIndicator.getAnimation() == null) {
                    Animation pulse = AnimationUtils.loadAnimation(context, R.anim.pulse_speaking);
                    binding.viewSpeakingIndicator.startAnimation(pulse);
                }
            } else {
                binding.viewSpeakingIndicator.clearAnimation();
                binding.viewSpeakingIndicator.setVisibility(View.GONE);
            }

            // Avatar – same pattern as the rest of the app
            bindAvatar(context, participant);
        }

        private void bindAvatar(Context context, VoiceParticipant participant) {
            int avatarSize = binding.ivAvatar.getLayoutParams() != null
                    ? binding.ivAvatar.getLayoutParams().width
                    : binding.ivAvatar.getWidth();
            Drawable avatarFallback = AvatarPlaceholderUtils.createAvatarDrawable(
                    context, participant.getDisplayName(), avatarSize);

            String avatarUrl = toAbsoluteUrl(participant.getAvatarUrl());

            Glide.with(context)
                    .load(avatarUrl)
                    .placeholder(avatarFallback)
                    .error(avatarFallback)
                    .fallback(avatarFallback)
                    .circleCrop()
                    .into(binding.ivAvatar);
        }

        @Nullable
        private String toAbsoluteUrl(@Nullable String url) {
            return NetworkConfig.resolveUrl(url);
        }
    }
}
