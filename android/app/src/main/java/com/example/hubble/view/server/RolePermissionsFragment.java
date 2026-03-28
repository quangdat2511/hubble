package com.example.hubble.view.server;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.hubble.R;
import com.example.hubble.adapter.server.RolePermissionAdapter;
import com.example.hubble.data.model.server.RoleMockData;
import com.example.hubble.data.model.server.RolePermissionSection;
import com.example.hubble.databinding.FragmentRolePermissionsBinding;

import java.util.List;

/**
 * Role permissions screen (#3,4,5). Shows permission toggles for @everyone or a custom role.
 */
public class RolePermissionsFragment extends Fragment {

    private FragmentRolePermissionsBinding binding;

    private String roleId;
    private String roleName;

    public static RolePermissionsFragment newInstance(String roleId, String roleName) {
        RolePermissionsFragment fragment = new RolePermissionsFragment();
        Bundle args = new Bundle();
        args.putString("role_id", roleId);
        args.putString("role_name", roleName);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentRolePermissionsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            roleId = getArguments().getString("role_id");
            roleName = getArguments().getString("role_name");
        }

        binding.toolbar.setTitle(roleName != null ? roleName : "@everyone");
        binding.toolbar.setSubtitle(getString(R.string.server_settings_roles));
        binding.toolbar.setNavigationOnClickListener(v -> requireActivity().onBackPressed());

        // Load permissions - using @everyone mock data for all roles for now
        List<RolePermissionSection> sections = RoleMockData.getEveryonePermissions(requireContext());

        RolePermissionAdapter adapter = new RolePermissionAdapter(sections);
        binding.rvPermissions.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvPermissions.setAdapter(adapter);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
