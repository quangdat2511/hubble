package com.example.hubble.view.server;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.hubble.data.model.server.RoleResponse;
import com.example.hubble.data.repository.RoleRepository;
import com.example.hubble.databinding.FragmentRolesEmptyBinding;
import com.example.hubble.viewmodel.RolesViewModel;
import com.example.hubble.viewmodel.RolesViewModelFactory;

/**
 * Empty roles screen (#6). Shown when server has no custom roles.
 * Provides a "Create Role" button and @everyone row.
 */
public class RolesEmptyFragment extends Fragment {

    private FragmentRolesEmptyBinding binding;
    private RolesViewModel viewModel;
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

        viewModel = new ViewModelProvider(requireActivity(),
                new RolesViewModelFactory(new RoleRepository(requireContext())))
                .get(RolesViewModel.class);

        binding.toolbar.setNavigationOnClickListener(v -> requireActivity().onBackPressed());

        binding.btnCreateRole.setOnClickListener(v ->
                ((ServerSettingsActivity) requireActivity()).navigateTo(
                        CreateRoleStep1Fragment.newInstance(serverId), true));

        // Find @everyone role id from loaded data
        binding.rowEveryone.setOnClickListener(v -> {
            String everyoneId = "everyone";
            if (viewModel.roles.getValue() != null
                    && viewModel.roles.getValue().isSuccess()
                    && viewModel.roles.getValue().getData() != null) {
                for (RoleResponse r : viewModel.roles.getValue().getData()) {
                    if (Boolean.TRUE.equals(r.getIsDefault())) {
                        everyoneId = r.getId();
                        break;
                    }
                }
            }
            ((ServerSettingsActivity) requireActivity()).navigateTo(
                    RolePermissionsFragment.newInstance(everyoneId, "@everyone", serverId), true);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
