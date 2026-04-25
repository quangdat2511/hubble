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
import androidx.lifecycle.ViewModelProvider;

import com.example.hubble.R;
import com.example.hubble.data.repository.RoleRepository;
import com.example.hubble.databinding.FragmentRoleDetailBinding;
import com.example.hubble.viewmodel.RolesViewModel;
import com.example.hubble.viewmodel.RolesViewModelFactory;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import java.util.HashMap;
import java.util.Map;

/**
 * Role detail / edit screen (#13). Shows role settings with name, color,
 * display/mention toggles, and sub-navigation to permissions, links, members.
 */
public class RoleDetailFragment extends Fragment {

    private FragmentRoleDetailBinding binding;
    private RolesViewModel viewModel;

    private String roleId;
    private String roleName;
    private int roleColor;
    private String serverId;

    public static RoleDetailFragment newInstance(String roleId, String roleName, int roleColor, String serverId) {
        RoleDetailFragment fragment = new RoleDetailFragment();
        Bundle args = new Bundle();
        args.putString("role_id", roleId);
        args.putString("role_name", roleName);
        args.putInt("role_color", roleColor);
        args.putString("server_id", serverId);
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
            serverId = getArguments().getString("server_id");
        }

        viewModel = new ViewModelProvider(requireActivity(),
                new RolesViewModelFactory(new RoleRepository(requireContext())))
                .get(RolesViewModel.class);

        binding.toolbar.setTitle(roleName);
        binding.toolbar.setSubtitle(getString(R.string.server_settings_roles));
        binding.toolbar.setNavigationOnClickListener(v -> {
            saveNameIfChanged();
            requireActivity().onBackPressed();
        });

        // Populate fields
        binding.etRoleName.setText(roleName);
        updateColorPreview();

        // Color hex display
        binding.tvColorHex.setText(String.format("#%06X", 0xFFFFFF & roleColor));

        // Prefetch members and permissions for sub-screens
        RoleMembersFragment.prefetch(requireContext(), serverId, roleId);
        RolePermissionsFragment.prefetch(requireContext(), serverId, roleId);

        // Color picker
        binding.rowColorPicker.setOnClickListener(v -> {
            ColorPickerBottomSheet sheet = ColorPickerBottomSheet.newInstance(roleColor);
            sheet.setOnColorSelectedListener(color -> {
                roleColor = color;
                updateColorPreview();
                binding.tvColorHex.setText(String.format("#%06X", 0xFFFFFF & color));
                // Save color immediately
                Map<String, Object> fields = new HashMap<>();
                fields.put("color", color);
                viewModel.updateRole(serverId, roleId, fields);
            });
            sheet.show(getChildFragmentManager(), "color_picker");
        });

        // Navigation rows
        binding.rowPermissions.setOnClickListener(v ->
                ((ServerSettingsActivity) requireActivity()).navigateTo(
                        RolePermissionsFragment.newInstance(roleId, roleName, serverId), true));

        binding.rowLinks.setOnClickListener(v ->
                Snackbar.make(view, R.string.main_coming_soon, Snackbar.LENGTH_SHORT).show());

        binding.rowMembers.setOnClickListener(v ->
                ((ServerSettingsActivity) requireActivity()).navigateTo(
                        RoleMembersFragment.newInstance(roleId, roleName, serverId), true));

        // Delete role
        binding.cardDeleteRole.setOnClickListener(v ->
                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle(R.string.role_detail_delete_confirm_title)
                        .setMessage(getString(R.string.role_detail_delete_confirm_message, roleName))
                        .setPositiveButton(R.string.role_detail_delete_confirm_action, (d, w) -> {
                            viewModel.deleteRole(serverId, roleId);
                        })
                        .setNegativeButton(R.string.cancel, null)
                        .show());

        // Observe delete result
        viewModel.deleteResult.observe(getViewLifecycleOwner(), result -> {
            if (result == null) return;
            if (result.isSuccess()) {
                viewModel.resetDeleteResult();
                // Reload roles list after deletion
                viewModel.loadRoles(serverId);
                requireActivity().onBackPressed();
            } else if (result.isError()) {
                Snackbar.make(view, result.getMessage(), Snackbar.LENGTH_SHORT).show();
                viewModel.resetDeleteResult();
            }
        });
    }

    private void saveNameIfChanged() {
        if (binding == null) return;
        String newName = binding.etRoleName.getText() != null
                ? binding.etRoleName.getText().toString().trim() : "";
        if (!newName.isEmpty() && !newName.equals(roleName)) {
            Map<String, Object> fields = new HashMap<>();
            fields.put("name", newName);
            viewModel.updateRole(serverId, roleId, fields);
        }
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
