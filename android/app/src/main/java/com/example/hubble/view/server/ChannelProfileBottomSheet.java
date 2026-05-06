package com.example.hubble.view.server;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.example.hubble.R;
import com.example.hubble.databinding.BottomSheetChannelProfileBinding;
import com.example.hubble.utils.TokenManager;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.snackbar.Snackbar;

public class ChannelProfileBottomSheet extends BottomSheetDialogFragment {

    private BottomSheetChannelProfileBinding binding;

    private static final String ARG_SERVER_ID = "server_id";
    private static final String ARG_SERVER_NAME = "server_name";
    private static final String ARG_SERVER_ICON_URL = "server_icon_url";
    private static final String ARG_CHANNEL_ID = "channel_id";
    private static final String ARG_CHANNEL_NAME = "channel_name";
    private static final String ARG_CHANNEL_TYPE = "channel_type";
    private static final String ARG_CHANNEL_PARENT_ID = "channel_parent_id";
    private static final String ARG_CHANNEL_IS_PRIVATE = "channel_is_private";
    private static final String ARG_CHANNEL_TOPIC = "channel_topic";
    private static final String ARG_SERVER_OWNER_ID = "server_owner_id";
    private static final String ARG_CHANNEL_PARENT_NAME = "channel_parent_name";
    private static final String ARG_CAN_MANAGE_CHANNELS = "can_manage_channels";

    private static final String ARG_CAN_INVITE_MEMBERS = "can_invite_members";

    public static ChannelProfileBottomSheet newInstance(String serverId, String serverName,
                                                        String serverIconUrl, String serverOwnerId,
                                                        String channelId, String channelName,
                                                        String channelType, String topic,
                                                        String parentId, String parentName,
                                                        boolean isPrivate, boolean canManageChannels) {
        return newInstance(serverId, serverName, serverIconUrl, serverOwnerId,
                channelId, channelName, channelType, topic, parentId, parentName,
                isPrivate, canManageChannels, false);
    }

    public static ChannelProfileBottomSheet newInstance(String serverId, String serverName,
                                                        String serverIconUrl, String serverOwnerId,
                                                        String channelId, String channelName,
                                                        String channelType, String topic,
                                                        String parentId, String parentName,
                                                        boolean isPrivate, boolean canManageChannels,
                                                        boolean canInviteMembers) {
        ChannelProfileBottomSheet sheet = new ChannelProfileBottomSheet();
        Bundle args = new Bundle();
        args.putString(ARG_SERVER_ID, serverId);
        args.putString(ARG_SERVER_NAME, serverName);
        args.putString(ARG_SERVER_ICON_URL, serverIconUrl);
        args.putString(ARG_SERVER_OWNER_ID, serverOwnerId);
        args.putString(ARG_CHANNEL_ID, channelId);
        args.putString(ARG_CHANNEL_NAME, channelName);
        args.putString(ARG_CHANNEL_TYPE, channelType);
        args.putString(ARG_CHANNEL_TOPIC, topic);
        args.putString(ARG_CHANNEL_PARENT_ID, parentId);
        args.putString(ARG_CHANNEL_PARENT_NAME, parentName);
        args.putBoolean(ARG_CHANNEL_IS_PRIVATE, isPrivate);
        args.putBoolean(ARG_CAN_MANAGE_CHANNELS, canManageChannels);
        args.putBoolean(ARG_CAN_INVITE_MEMBERS, canInviteMembers);
        sheet.setArguments(args);
        return sheet;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = BottomSheetChannelProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (getArguments() == null) return;

        String serverId = getArguments().getString(ARG_SERVER_ID);
        String serverName = getArguments().getString(ARG_SERVER_NAME);
        String serverIconUrl = getArguments().getString(ARG_SERVER_ICON_URL);
        String serverOwnerId = getArguments().getString(ARG_SERVER_OWNER_ID);
        String channelId = getArguments().getString(ARG_CHANNEL_ID);
        String channelName = getArguments().getString(ARG_CHANNEL_NAME);
        String channelType = getArguments().getString(ARG_CHANNEL_TYPE);
        String topic = getArguments().getString(ARG_CHANNEL_TOPIC);
        String parentId = getArguments().getString(ARG_CHANNEL_PARENT_ID);
        String parentName = getArguments().getString(ARG_CHANNEL_PARENT_NAME);
        boolean isPrivate = getArguments().getBoolean(ARG_CHANNEL_IS_PRIVATE, false);
        boolean canManageChannels = getArguments().getBoolean(ARG_CAN_MANAGE_CHANNELS, false);
        boolean canInviteMembers = getArguments().getBoolean(ARG_CAN_INVITE_MEMBERS, false);

        // Owner-only: hide Edit/Duplicate for non-owners
        TokenManager tm = new TokenManager(requireContext());
        String currentUserId = tm.getUser() != null ? tm.getUser().getId() : null;
        boolean isOwner = currentUserId != null && currentUserId.equals(serverOwnerId);
        binding.cardCluster3.setVisibility((isOwner || canManageChannels) ? View.VISIBLE : View.GONE);

        // Invite: owner or members with INVITE_MEMBERS permission
        if (!isOwner && !canInviteMembers) {
            binding.rowInvite.setVisibility(View.GONE);
        }

        // Header
        String prefix = "TEXT".equalsIgnoreCase(channelType) ? "# " : "";
        binding.tvChannelName.setText(prefix + channelName);

        if (serverIconUrl != null && !serverIconUrl.isEmpty()) {
            binding.ivServerIcon.setVisibility(View.VISIBLE);
            binding.tvServerInitials.setVisibility(View.GONE);
            Glide.with(this).load(serverIconUrl)
                    .placeholder(R.color.color_primary)
                    .into(binding.ivServerIcon);
        } else {
            binding.ivServerIcon.setVisibility(View.GONE);
            binding.tvServerInitials.setVisibility(View.VISIBLE);
            binding.tvServerInitials.setText(
                    serverName != null && !serverName.isEmpty()
                            ? serverName.substring(0, 1).toUpperCase() : "?");
            binding.tvServerInitials.setBackgroundResource(R.drawable.bg_server_icon_initials_rounded);
        }

        // Cluster 1
        binding.rowInvite.setOnClickListener(v -> {
            dismiss();
            InvitePeopleBottomSheet.newInstance(serverId, serverName)
                    .show(requireActivity().getSupportFragmentManager(), "invite_people");
        });
        binding.rowCopyLink.setOnClickListener(v -> showComingSoon());

        // Cluster 2
        binding.rowMarkAsRead.setOnClickListener(v -> showComingSoon());
        binding.rowNotifications.setOnClickListener(v -> showComingSoon());

        // Cluster 3
        binding.rowEditChannel.setOnClickListener(v -> {
            dismiss();
            startActivity(ChannelSettingsActivity.createIntent(
                    requireContext(), serverId, channelId, channelName, channelType,
                    topic, parentId, parentName, isPrivate));
        });
        binding.rowDuplicateChannel.setOnClickListener(v -> {
            dismiss();
            startActivity(DuplicateChannelActivity.createIntent(
                    requireContext(), serverId, channelName, channelType, parentId, isPrivate));
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        BottomSheetDialog dialog = (BottomSheetDialog) getDialog();
        if (dialog != null) {
            View bottomSheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                bottomSheet.setBackgroundResource(R.drawable.bg_bottom_sheet_top_rounded);
            }
            BottomSheetBehavior<?> behavior = dialog.getBehavior();
            behavior.setPeekHeight(BottomSheetBehavior.PEEK_HEIGHT_AUTO);
            behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            behavior.setSkipCollapsed(true);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void showComingSoon() {
        Snackbar.make(binding.getRoot(), R.string.main_coming_soon, Snackbar.LENGTH_SHORT).show();
    }
}
