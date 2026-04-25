package com.example.hubble.view.server;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.example.hubble.R;
import com.example.hubble.databinding.BottomSheetCategoryProfileBinding;
import com.example.hubble.utils.TokenManager;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.snackbar.Snackbar;

public class CategoryProfileBottomSheet extends BottomSheetDialogFragment {

    private BottomSheetCategoryProfileBinding binding;

    private static final String ARG_SERVER_ID = "server_id";
    private static final String ARG_SERVER_NAME = "server_name";
    private static final String ARG_SERVER_ICON_URL = "server_icon_url";
    private static final String ARG_SERVER_OWNER_ID = "server_owner_id";
    private static final String ARG_CATEGORY_ID = "category_id";
    private static final String ARG_CATEGORY_NAME = "category_name";
    private static final String ARG_CATEGORY_IS_PRIVATE = "category_is_private";

    public static CategoryProfileBottomSheet newInstance(
            String serverId, String serverName, String serverIconUrl, String serverOwnerId,
            String categoryId, String categoryName, boolean isPrivate) {
        CategoryProfileBottomSheet sheet = new CategoryProfileBottomSheet();
        Bundle args = new Bundle();
        args.putString(ARG_SERVER_ID, serverId);
        args.putString(ARG_SERVER_NAME, serverName);
        args.putString(ARG_SERVER_ICON_URL, serverIconUrl);
        args.putString(ARG_SERVER_OWNER_ID, serverOwnerId);
        args.putString(ARG_CATEGORY_ID, categoryId);
        args.putString(ARG_CATEGORY_NAME, categoryName);
        args.putBoolean(ARG_CATEGORY_IS_PRIVATE, isPrivate);
        sheet.setArguments(args);
        return sheet;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = BottomSheetCategoryProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (getArguments() == null) { dismiss(); return; }

        String serverId = getArguments().getString(ARG_SERVER_ID);
        String serverName = getArguments().getString(ARG_SERVER_NAME);
        String serverIconUrl = getArguments().getString(ARG_SERVER_ICON_URL);
        String serverOwnerId = getArguments().getString(ARG_SERVER_OWNER_ID);
        String categoryId = getArguments().getString(ARG_CATEGORY_ID);
        String categoryName = getArguments().getString(ARG_CATEGORY_NAME);
        boolean isPrivate = getArguments().getBoolean(ARG_CATEGORY_IS_PRIVATE, false);

        // Header
        binding.tvCategoryName.setText(categoryName);

        if (serverIconUrl != null && !serverIconUrl.isEmpty()) {
            binding.ivServerIcon.setVisibility(View.VISIBLE);
            Glide.with(this).load(serverIconUrl).into(binding.ivServerIcon);
        } else if (serverName != null && !serverName.isEmpty()) {
            binding.tvServerInitials.setVisibility(View.VISIBLE);
            binding.tvServerInitials.setText(serverName.substring(0, Math.min(3, serverName.length())));
            binding.tvServerInitials.setBackgroundResource(R.drawable.bg_server_icon_initials_rounded);
        }

        // Owner check
        TokenManager tm = new TokenManager(requireContext());
        String currentUserId = tm.getUser() != null ? tm.getUser().getId() : null;
        boolean isOwner = currentUserId != null && currentUserId.equals(serverOwnerId);
        binding.cardCluster3.setVisibility(isOwner ? View.VISIBLE : View.GONE);

        // Cluster 1: placeholders
        binding.rowMarkAsRead.setOnClickListener(v -> showComingSoon());
        binding.rowNotifications.setOnClickListener(v -> showComingSoon());

        // Cluster 2 (owner only): Edit category
        binding.rowEditCategory.setOnClickListener(v -> {
            dismiss();
            requireContext().startActivity(
                    CategorySettingsActivity.createIntent(requireContext(),
                            serverId, categoryId, categoryName, isPrivate));
        });

        // Cluster 3: Create channel in this category
        binding.rowCreateChannel.setOnClickListener(v -> {
            dismiss();
            requireContext().startActivity(
                    CreateChannelActivity.createIntent(requireContext(), serverId, categoryId));
        });
    }

    private void showComingSoon() {
        if (getView() != null) {
            Snackbar.make(getView(), R.string.main_coming_soon, Snackbar.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() instanceof BottomSheetDialog) {
            BottomSheetDialog dialog = (BottomSheetDialog) getDialog();
            View bottomSheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                bottomSheet.setBackgroundResource(R.drawable.bg_bottom_sheet_top_rounded);
                BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheet);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                behavior.setSkipCollapsed(true);
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
