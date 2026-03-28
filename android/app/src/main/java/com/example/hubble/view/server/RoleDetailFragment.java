package com.example.hubble.view.server;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.hubble.R;
import com.example.hubble.databinding.FragmentRoleDetailBinding;
import com.google.android.material.snackbar.Snackbar;

/**
 * Role detail / edit screen (#13). Shows role settings with name, color,
 * display/mention toggles, and sub-navigation to permissions, links, members.
 */
public class RoleDetailFragment extends Fragment {

    private FragmentRoleDetailBinding binding;

    private String roleId;
    private String roleName;
    private int roleColor;

    public static RoleDetailFragment newInstance(String roleId, String roleName, int roleColor) {
        RoleDetailFragment fragment = new RoleDetailFragment();
        Bundle args = new Bundle();
        args.putString("role_id", roleId);
        args.putString("role_name", roleName);
        args.putInt("role_color", roleColor);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentRoleDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            roleId = getArguments().getString("role_id");
            roleName = getArguments().getString("role_name");
            roleColor = getArguments().getInt("role_color", Color.parseColor("#99AAB5"));
        }

        binding.toolbar.setTitle(roleName);
        binding.toolbar.setSubtitle(getString(R.string.server_settings_roles));
        binding.toolbar.setNavigationOnClickListener(v -> requireActivity().onBackPressed());

        // Populate fields
        binding.etRoleName.setText(roleName);
        updateColorPreview();

        // Color hex display
        binding.tvColorHex.setText(String.format("#%06X", 0xFFFFFF & roleColor));

        // Color picker row (demo: just show coming soon)
        binding.rowColorPicker.setOnClickListener(v ->
                Snackbar.make(view, R.string.main_coming_soon, Snackbar.LENGTH_SHORT).show());

        // Navigation rows
        binding.rowPermissions.setOnClickListener(v ->
                ((ServerSettingsActivity) requireActivity()).navigateTo(
                        RolePermissionsFragment.newInstance(roleId, roleName), true));

        binding.rowLinks.setOnClickListener(v ->
                Snackbar.make(view, R.string.main_coming_soon, Snackbar.LENGTH_SHORT).show());

        binding.rowMembers.setOnClickListener(v ->
                Snackbar.make(view, R.string.main_coming_soon, Snackbar.LENGTH_SHORT).show());

        // Delete role
        binding.cardDeleteRole.setOnClickListener(v ->
                Snackbar.make(view, R.string.main_coming_soon, Snackbar.LENGTH_SHORT).show());
    }

    private void updateColorPreview() {
        GradientDrawable circle = new GradientDrawable();
        circle.setShape(GradientDrawable.OVAL);
        circle.setColor(roleColor);
        binding.viewColorPreview.setBackground(circle);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
