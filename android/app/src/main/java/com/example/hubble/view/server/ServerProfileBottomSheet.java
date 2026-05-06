package com.example.hubble.view.server;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.example.hubble.R;
import com.example.hubble.utils.AvatarPlaceholderUtils;
import com.example.hubble.utils.TokenManager;
import com.example.hubble.data.model.server.ServerItem;
import com.example.hubble.databinding.BottomSheetServerProfileBinding;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class ServerProfileBottomSheet extends BottomSheetDialogFragment {
    private BottomSheetServerProfileBinding binding;
    private int memberCount;
    private int onlineCount;
    private String serverId;

    public static ServerProfileBottomSheet newInstance(ServerItem server, int memberCount, int onlineCount) {
        return newInstance(server, memberCount, onlineCount, false, false);
    }

    public static ServerProfileBottomSheet newInstance(ServerItem server, int memberCount, int onlineCount,
                                                        boolean canManageChannels, boolean canInviteMembers) {
        ServerProfileBottomSheet fragment = new ServerProfileBottomSheet();
        Bundle args = new Bundle();
        args.putString("server_id",       server.getId());
        args.putString("server_name",     server.getName());
        args.putString("server_icon_url", server.getIconUrl());
        args.putString("owner_id",        server.getOwnerId());
        args.putString("description",     server.getDescription());
        args.putInt("member_count",  memberCount);
        args.putInt("online_count",  onlineCount);
        args.putBoolean("can_manage_channels", canManageChannels);
        args.putBoolean("can_invite_members", canInviteMembers);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = BottomSheetServerProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            serverId = getArguments().getString("server_id");
            String serverName = getArguments().getString("server_name");
            String iconUrl    = getArguments().getString("server_icon_url");
            String ownerId    = getArguments().getString("owner_id");
            String description = getArguments().getString("description");
            memberCount = getArguments().getInt("member_count", 0);
            onlineCount = getArguments().getInt("online_count", 0);

            binding.tvServerName.setText(serverName);
            binding.tvOnlineCount.setText(
                    getString(R.string.server_profile_online_count, onlineCount));
            binding.tvMemberCount.setText(
                    getString(R.string.server_profile_member_count_only, memberCount));

            // Load icon or show default avatar fallback
            binding.tvServerInitials.setVisibility(View.GONE);
            binding.ivServerIcon.setVisibility(View.VISIBLE);
            if (iconUrl != null && !iconUrl.isEmpty()) {
                Glide.with(this)
                        .load(iconUrl)
                        .placeholder(AvatarPlaceholderUtils.createServerAvatarDrawable(
                                requireContext(), serverName, 0))
                        .into(binding.ivServerIcon);
            } else {
                binding.ivServerIcon.setImageDrawable(
                        AvatarPlaceholderUtils.createServerAvatarDrawable(
                                requireContext(), serverName, 0));
            }

            // Settings button — now passes ownerId + iconUrl
            binding.btnSettings.setOnClickListener(v -> {
                dismiss();
                startActivity(ServerSettingsActivity.createIntent(
                        requireContext(), serverId, serverName, ownerId, iconUrl, description));
            });

            // Quick action buttons
            binding.rowInvitePeople.setOnClickListener(v -> {
                dismiss();
                InvitePeopleBottomSheet.newInstance(serverId, serverName)
                        .show(requireActivity().getSupportFragmentManager(), "invite_people");
            });
            binding.rowNotificationsQuick.setOnClickListener(v -> showComingSoon());
            binding.rowSettingsQuick.setOnClickListener(v -> {
                dismiss();
                startActivity(ServerSettingsActivity.createIntent(
                        requireContext(), serverId, serverName, ownerId, iconUrl, description));
            });

            // Show create actions card to owner or members with MANAGE_CHANNELS permission
            TokenManager tokenManager = new TokenManager(requireContext());
            String currentUserId = tokenManager.getUser() != null ? tokenManager.getUser().getId() : null;
            boolean isOwner = ownerId != null && ownerId.equals(currentUserId);
            boolean canManageChannels = getArguments().getBoolean("can_manage_channels", false);
            boolean canInviteMembers = getArguments().getBoolean("can_invite_members", false);
            if (isOwner || canManageChannels) {
                binding.cardCreateActions.setVisibility(View.VISIBLE);
            } else {
                binding.cardCreateActions.setVisibility(View.GONE);
            }

            // Invite button: owner or members with INVITE_MEMBERS permission
            if (!isOwner && !canInviteMembers) {
                binding.rowInvitePeople.setVisibility(View.GONE);
            }

            // List items
            binding.rowMarkAsRead.setOnClickListener(v -> showComingSoon());
            binding.rowCreateChannel.setOnClickListener(v -> {
                dismiss();
                startActivity(CreateChannelActivity.createIntent(requireContext(), serverId));
            });
            binding.rowCreateCategory.setOnClickListener(v -> {
                dismiss();
                startActivity(CreateCategoryActivity.createIntent(requireContext(), serverId));
            });
            binding.rowEditServerProfile.setOnClickListener(v -> showComingSoon());
        }
    }

    public String getServerIdArg() {
        return serverId;
    }

    public void updateMemberStats(int memberCount, int onlineCount) {
        this.memberCount = Math.max(memberCount, 0);
        this.onlineCount = Math.max(onlineCount, 0);
        if (binding == null) {
            return;
        }
        binding.tvOnlineCount.setText(
                getString(R.string.server_profile_online_count, this.onlineCount));
        binding.tvMemberCount.setText(
                getString(R.string.server_profile_member_count_only, this.memberCount));
    }

    @Override
    public void onStart() {
        super.onStart();
        BottomSheetDialog dialog = (BottomSheetDialog) getDialog();
        if (dialog != null) {
            BottomSheetBehavior<?> behavior = dialog.getBehavior();
            behavior.setPeekHeight(BottomSheetBehavior.PEEK_HEIGHT_AUTO);
            behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            behavior.setSkipCollapsed(true);
            FrameLayout bottomSheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                bottomSheet.setBackgroundResource(R.drawable.bg_bottom_sheet_top_rounded);
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void showComingSoon() {
        com.google.android.material.snackbar.Snackbar.make(binding.getRoot(),
                R.string.main_coming_soon,
                com.google.android.material.snackbar.Snackbar.LENGTH_SHORT).show();
    }
}
