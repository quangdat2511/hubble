package com.example.hubble.view.server;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.example.hubble.R;
import com.example.hubble.data.model.auth.AuthResult;
import com.example.hubble.data.model.server.ServerMemberItem;
import com.example.hubble.data.model.server.ServerRoleItem;
import com.example.hubble.databinding.BottomSheetMemberDetailBinding;
import com.example.hubble.utils.TokenManager;
import com.example.hubble.viewmodel.server.ServerSettingsViewModel;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

public class MemberDetailBottomSheet extends BottomSheetDialogFragment {
    private BottomSheetMemberDetailBinding binding;
    private ServerSettingsViewModel viewModel;
    private ServerMemberItem member;
    private String serverId;
    private boolean isCurrentUserOwner;

    public static MemberDetailBottomSheet newInstance(ServerMemberItem member, String serverId) {
        MemberDetailBottomSheet fragment = new MemberDetailBottomSheet();
        Bundle args = new Bundle();
        args.putString("user_id", member.getUserId());
        args.putString("username", member.getUsername());
        args.putString("display_name", member.getDisplayName());
        args.putString("avatar_url", member.getAvatarUrl());
        args.putInt("avatar_bg_color", member.getAvatarBackgroundColor());
        args.putString("status", member.getStatus());
        args.putBoolean("is_owner", member.isOwner());
        args.putString("server_id", serverId);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = BottomSheetMemberDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() == null) return;

        // Parse arguments
        String userId = getArguments().getString("user_id");
        String username = getArguments().getString("username");
        String displayName = getArguments().getString("display_name");
        String avatarUrl = getArguments().getString("avatar_url");
        int bgColor = getArguments().getInt("avatar_bg_color");
        String status = getArguments().getString("status");
        boolean isOwner = getArguments().getBoolean("is_owner");
        serverId = getArguments().getString("server_id");

        // Rebuild member object (simplified version)
        member = new ServerMemberItem(userId, username, displayName, avatarUrl, bgColor, null, status, isOwner);

        // Get ViewModel shared with Activity
        viewModel = new ViewModelProvider(requireActivity()).get(ServerSettingsViewModel.class);

        // Get current user ID
        TokenManager tokenManager = new TokenManager(requireContext());
        String currentUserId = tokenManager.getUser() != null ? tokenManager.getUser().getId() : "";
        
        // Find if current user is owner in the member list
        boolean isMeOwner = false;
        if (viewModel.getMembersState().getValue() != null && 
            viewModel.getMembersState().getValue().getData() != null) {
            for (ServerMemberItem m : viewModel.getMembersState().getValue().getData()) {
                if (m.getUserId().equals(currentUserId) && m.isOwner()) {
                    isMeOwner = true;
                    break;
                }
            }
        }
        this.isCurrentUserOwner = isMeOwner;

        // Is looking at self?
        boolean isSelf = userId.equals(currentUserId);

        // Populate UI
        String displayText = displayName != null && !displayName.isEmpty() ? displayName : username;
        binding.tvMemberUsername.setText(displayText);
        binding.tvMemberDiscriminator.setText("#" + userId.substring(0, Math.min(4, Math.max(0, userId.length()))));

        // Avatar
        if (avatarUrl != null && !avatarUrl.isEmpty()) {
            binding.ivMemberAvatar.setVisibility(View.VISIBLE);
            binding.tvMemberAvatarInitials.setVisibility(View.GONE);
            // TODO: Load with Glide when implemented
        } else {
            binding.ivMemberAvatar.setVisibility(View.GONE);
            binding.tvMemberAvatarInitials.setVisibility(View.VISIBLE);
            binding.tvMemberAvatarInitials.setText(member.getDisplayInitials());
            binding.tvMemberAvatarInitials.setBackgroundColor(bgColor);
        }

        // Online status
        int statusColor = getStatusColor(status);
        if ("OFFLINE".equalsIgnoreCase(status)) {
            binding.viewMemberStatus.setVisibility(View.GONE);
        } else {
            binding.viewMemberStatus.setVisibility(View.VISIBLE);
            binding.viewMemberStatus.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(statusColor));
        }

        // Action buttons labels
        binding.tvKickLabel.setText(getString(R.string.member_action_kick, username));
        binding.tvBanLabel.setText(getString(R.string.member_action_ban, username));

        // Visibility of actions
        if (isSelf) {
            // Hide all server management actions for self
            binding.rowKick.setVisibility(View.GONE);
            binding.rowBan.setVisibility(View.GONE);
            binding.rowTransferOwnership.setVisibility(View.GONE);
            binding.dividerTransferOwnership.setVisibility(View.GONE);
            binding.rowManageRoles.setVisibility(View.GONE);
        } else if (!isMeOwner) {
            // Hide owner actions if current user is not owner
            binding.rowKick.setVisibility(View.GONE);
            binding.rowBan.setVisibility(View.GONE);
            binding.rowTransferOwnership.setVisibility(View.GONE);
            binding.dividerTransferOwnership.setVisibility(View.GONE);
            binding.rowManageRoles.setVisibility(View.GONE);
        } else {
            // Current user is owner
            binding.rowKick.setVisibility(View.VISIBLE);
            binding.rowBan.setVisibility(View.VISIBLE);
            binding.rowManageRoles.setVisibility(View.VISIBLE);
            
            // Show transfer ownership ONLY if target is NOT already the owner (though owner shouldn't be clicking self anyway)
            if (!isOwner) {
                binding.rowTransferOwnership.setVisibility(View.VISIBLE);
                binding.dividerTransferOwnership.setVisibility(View.VISIBLE);
            } else {
                binding.rowTransferOwnership.setVisibility(View.GONE);
                binding.dividerTransferOwnership.setVisibility(View.GONE);
                // Also can't kick/ban the owner (self or other owner if multiple exist, though usually one)
                binding.rowKick.setVisibility(View.GONE);
                binding.rowBan.setVisibility(View.GONE);
            }
        }

        // Click listeners
        binding.rowKick.setOnClickListener(v -> confirmAndKick(username));
        binding.rowBan.setOnClickListener(v -> confirmAndBan(username));
        binding.rowTransferOwnership.setOnClickListener(v -> confirmAndTransfer(username));
        binding.rowManageRoles.setOnClickListener(v -> {
            Snackbar.make(binding.getRoot(), R.string.main_coming_soon, Snackbar.LENGTH_SHORT).show();
        });
        binding.btnMessage.setOnClickListener(v -> {
            dismiss();
            Snackbar.make(binding.getRoot(), R.string.main_coming_soon, Snackbar.LENGTH_SHORT).show();
        });

        // Observe action states
        viewModel.getKickState().observe(getViewLifecycleOwner(), result -> {
            if (result != null && result.getStatus() == AuthResult.Status.SUCCESS) {
                viewModel.consumeKickState();
                dismiss();
                Snackbar.make(requireActivity().findViewById(android.R.id.content),
                        getString(R.string.member_kicked_success, username),
                        Snackbar.LENGTH_SHORT).show();
            }
        });

        viewModel.getBanState().observe(getViewLifecycleOwner(), result -> {
            if (result != null && result.getStatus() == AuthResult.Status.SUCCESS) {
                viewModel.consumeBanState();
                dismiss();
                Snackbar.make(requireActivity().findViewById(android.R.id.content),
                        getString(R.string.member_banned_success, username),
                        Snackbar.LENGTH_SHORT).show();
            }
        });

        viewModel.getTransferOwnershipState().observe(getViewLifecycleOwner(), result -> {
            if (result != null && result.getStatus() == AuthResult.Status.SUCCESS) {
                viewModel.consumeTransferOwnershipState();
                dismiss();
                Snackbar.make(requireActivity().findViewById(android.R.id.content),
                        R.string.main_coming_soon,
                        Snackbar.LENGTH_SHORT).show();
            }
        });
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
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void confirmAndKick(String username) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.member_kick_confirm_title)
                .setMessage(getString(R.string.member_kick_confirm_message, username))
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    if (member != null) {
                        viewModel.kickMember(member.getUserId());
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void confirmAndBan(String username) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.member_ban_confirm_title)
                .setMessage(getString(R.string.member_ban_confirm_message, username))
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    if (member != null) {
                        viewModel.banMember(member.getUserId());
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void confirmAndTransfer(String username) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.member_transfer_confirm_title)
                .setMessage(R.string.member_transfer_confirm_message)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    if (member != null) {
                        viewModel.transferOwnership(member.getUserId());
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private int getStatusColor(String status) {
        if ("ONLINE".equalsIgnoreCase(status)) {
            return requireContext().getColor(R.color.color_online);
        } else if ("IDLE".equalsIgnoreCase(status)) {
            return requireContext().getColor(R.color.color_idle);
        } else if ("DND".equalsIgnoreCase(status)) {
            return requireContext().getColor(R.color.color_dnd);
        } else {
            return requireContext().getColor(R.color.color_offline);
        }
    }
}
