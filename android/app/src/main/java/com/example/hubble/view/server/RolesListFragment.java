package com.example.hubble.view.server;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.hubble.R;
import com.example.hubble.adapter.server.RoleAdapter;
import com.example.hubble.data.model.server.RoleResponse;
import com.example.hubble.data.model.server.ServerRoleItem;
import com.example.hubble.data.repository.RoleRepository;
import com.example.hubble.databinding.FragmentRolesListBinding;
import com.example.hubble.viewmodel.RolesViewModel;
import com.example.hubble.viewmodel.RolesViewModelFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Roles list screen (#2). Shows @everyone + custom roles.
 * Navigates to RolesEmptyFragment if no custom roles exist.
 */
public class RolesListFragment extends Fragment {

    private FragmentRolesListBinding binding;
    private final List<ServerRoleItem> customRoles = new ArrayList<>();
    private RolesViewModel viewModel;
    private String serverId;

    public static RolesListFragment newInstance(String serverId) {
        RolesListFragment fragment = new RolesListFragment();
        Bundle args = new Bundle();
        args.putString("server_id", serverId);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentRolesListBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            serverId = getArguments().getString("server_id");
        }

        viewModel = new ViewModelProvider(requireActivity(),
                new RolesViewModelFactory(new RoleRepository(requireContext())))
                .get(RolesViewModel.class);

        binding.toolbar.setNavigationOnClickListener(v -> requireActivity().onBackPressed());

        setupClickListeners();
        observeRoles();

        viewModel.loadRoles(serverId);
    }

    private void observeRoles() {
        viewModel.roles.observe(getViewLifecycleOwner(), result -> {
            if (result == null) return;
            if (result.isSuccess() && result.getData() != null) {
                customRoles.clear();
                String everyoneRoleId = null;
                for (RoleResponse r : result.getData()) {
                    if (Boolean.TRUE.equals(r.getIsDefault())) {
                        everyoneRoleId = r.getId();
                        continue;
                    }
                    customRoles.add(new ServerRoleItem(
                            r.getId(), r.getName(),
                            r.getColor() != null ? r.getColor() : 0));
                }
                if (customRoles.isEmpty()) {
                    ((ServerSettingsActivity) requireActivity()).navigateTo(
                            RolesEmptyFragment.newInstance(serverId), false);
                    return;
                }
                setupRolesList();
                // Store everyone role id for navigation
                final String eid = everyoneRoleId;
                binding.rowEveryone.setOnClickListener(v ->
                        ((ServerSettingsActivity) requireActivity()).navigateTo(
                                RolePermissionsFragment.newInstance(
                                        eid != null ? eid : "everyone", "@everyone", serverId),
                                true));
            }
        });
    }

    private void setupRolesList() {
        binding.tvRolesCount.setText(getString(R.string.roles_count, customRoles.size()));

        RoleAdapter adapter = new RoleAdapter(customRoles, role ->
                ((ServerSettingsActivity) requireActivity()).navigateTo(
                        RoleDetailFragment.newInstance(role.getId(), role.getName(), role.getColor(), serverId),
                        true));

        binding.rvRoles.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvRoles.setAdapter(adapter);
    }

    private void setupClickListeners() {
        // Add role
        binding.btnAddRole.setOnClickListener(v ->
                ((ServerSettingsActivity) requireActivity()).navigateTo(
                        CreateRoleStep1Fragment.newInstance(serverId), true));

        // Reorder
        binding.btnReorder.setOnClickListener(v -> {
            // TODO: implement reorder UI
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
