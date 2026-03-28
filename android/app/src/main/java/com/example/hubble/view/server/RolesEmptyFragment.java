package com.example.hubble.view.server;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.hubble.databinding.FragmentRolesEmptyBinding;

/**
 * Empty roles screen (#6). Shown when server has no custom roles.
 * Provides a "Create Role" button and @everyone row.
 */
public class RolesEmptyFragment extends Fragment {

    private FragmentRolesEmptyBinding binding;
    private String serverId;

    public static RolesEmptyFragment newInstance(String serverId) {
        RolesEmptyFragment fragment = new RolesEmptyFragment();
        Bundle args = new Bundle();
        args.putString("server_id", serverId);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentRolesEmptyBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            serverId = getArguments().getString("server_id");
        }

        binding.toolbar.setNavigationOnClickListener(v -> requireActivity().onBackPressed());

        binding.btnCreateRole.setOnClickListener(v ->
                ((ServerSettingsActivity) requireActivity()).navigateTo(
                        CreateRoleStep1Fragment.newInstance(serverId), true));

        binding.rowEveryone.setOnClickListener(v ->
                ((ServerSettingsActivity) requireActivity()).navigateTo(
                        RolePermissionsFragment.newInstance("everyone", "@everyone"), true));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
