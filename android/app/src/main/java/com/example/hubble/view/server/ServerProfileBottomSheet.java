package com.example.hubble.view.server;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.hubble.R;
import com.example.hubble.data.model.server.ServerItem;
import com.example.hubble.databinding.BottomSheetServerProfileBinding;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.gson.Gson;

public class ServerProfileBottomSheet extends BottomSheetDialogFragment {
    private BottomSheetServerProfileBinding binding;
    private ServerItem server;
    private int memberCount;
    private int onlineCount;

    public static ServerProfileBottomSheet newInstance(ServerItem server, int memberCount, int onlineCount) {
        ServerProfileBottomSheet fragment = new ServerProfileBottomSheet();
        Bundle args = new Bundle();
        args.putString("server_id", server.getId());
        args.putString("server_name", server.getName());
        args.putString("server_icon_url", server.getIconUrl());
        args.putInt("member_count", memberCount);
        args.putInt("online_count", onlineCount);
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
            String serverId = getArguments().getString("server_id");
            String serverName = getArguments().getString("server_name");
            String iconUrl = getArguments().getString("server_icon_url");
            memberCount = getArguments().getInt("member_count", 0);
            onlineCount = getArguments().getInt("online_count", 0);

            // Populate server info
            binding.tvServerName.setText(serverName);
            binding.tvOnlineCount.setText(
                    getString(R.string.server_profile_online_count, onlineCount));
            binding.tvMemberCount.setText(
                    getString(R.string.server_profile_member_count_only, memberCount));

            // Load icon or show initials
            if (iconUrl != null && !iconUrl.isEmpty()) {
                binding.ivServerIcon.setVisibility(View.VISIBLE);
                binding.tvServerInitials.setVisibility(View.GONE);
                // TODO: Load with Glide when implemented
            } else {
                binding.ivServerIcon.setVisibility(View.GONE);
                binding.tvServerInitials.setVisibility(View.VISIBLE);
                binding.tvServerInitials.setText(serverName.substring(0, 1).toUpperCase());
                binding.tvServerInitials.setBackgroundColor(
                        getResources().getColor(R.color.color_primary, null)
                );
            }

            // Settings gear button (header)
            binding.btnSettings.setOnClickListener(v -> {
                dismiss();
                startActivity(ServerSettingsActivity.createIntent(requireContext(), serverId, serverName));
            });

            // ── Quick action buttons ──────────────────────────────────────
            binding.rowUpgrade.setOnClickListener(v -> showComingSoon());

            // Lời mời → opens Discord-style InvitePeopleBottomSheet
            binding.rowInvitePeople.setOnClickListener(v -> {
                dismiss();
                InvitePeopleBottomSheet.newInstance(serverId, serverName)
                        .show(requireActivity().getSupportFragmentManager(), "invite_people");
            });

            binding.rowNotificationsQuick.setOnClickListener(v -> showComingSoon());
            binding.rowSettingsQuick.setOnClickListener(v -> {
                dismiss();
                startActivity(ServerSettingsActivity.createIntent(requireContext(), serverId, serverName));
            });

            // ── List items ────────────────────────────────────────────────
            binding.rowMarkAsRead.setOnClickListener(v -> showComingSoon());
            binding.rowCreateChannel.setOnClickListener(v -> showComingSoon());
            binding.rowCreateCategory.setOnClickListener(v -> showComingSoon());
            binding.rowCreateEvent.setOnClickListener(v -> showComingSoon());
            binding.rowEditServerProfile.setOnClickListener(v -> showComingSoon());
            binding.rowHideMuted.setOnClickListener(v ->
                    binding.switchHideMuted.setChecked(!binding.switchHideMuted.isChecked()));

        }
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

    private void showComingSoon() {
        com.google.android.material.snackbar.Snackbar.make(binding.getRoot(),
                R.string.main_coming_soon, com.google.android.material.snackbar.Snackbar.LENGTH_SHORT).show();
    }
}
