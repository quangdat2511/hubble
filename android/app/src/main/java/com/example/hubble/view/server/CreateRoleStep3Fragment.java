package com.example.hubble.view.server;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.hubble.R;
import com.example.hubble.adapter.server.MemberSelectAdapter;
import com.example.hubble.data.model.server.MemberBriefResponse;
import com.example.hubble.data.model.server.ServerMemberItem;
import com.example.hubble.data.repository.RoleRepository;
import com.example.hubble.data.repository.ServerMemberRepository;
import com.example.hubble.databinding.FragmentCreateRoleStep3Binding;
import com.example.hubble.viewmodel.RolesViewModel;
import com.example.hubble.viewmodel.RolesViewModelFactory;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

/**
 * Create Role Step 3 (#12) - Assign members to the new role.
 */
public class CreateRoleStep3Fragment extends Fragment {

    private FragmentCreateRoleStep3Binding binding;
    private RolesViewModel viewModel;
    private MemberSelectAdapter adapter;

    private String serverId;
    private String roleName;
    private int roleColor;
    private String preset;

    public static CreateRoleStep3Fragment newInstance(String serverId, String roleName,
                                                      int roleColor, String preset) {
        CreateRoleStep3Fragment fragment = new CreateRoleStep3Fragment();
        Bundle args = new Bundle();
        args.putString("server_id", serverId);
        args.putString("role_name", roleName);
        args.putInt("role_color", roleColor);
        args.putString("preset", preset);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentCreateRoleStep3Binding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            serverId = getArguments().getString("server_id");
            roleName = getArguments().getString("role_name");
            roleColor = getArguments().getInt("role_color");
            preset = getArguments().getString("preset");
        }

        viewModel = new ViewModelProvider(requireActivity(),
                new RolesViewModelFactory(new RoleRepository(requireContext())))
                .get(RolesViewModel.class);

        binding.toolbar.setTitle(getString(R.string.create_role_step, 3));
        binding.toolbar.setNavigationOnClickListener(v -> requireActivity().onBackPressed());

        adapter = new MemberSelectAdapter(new ArrayList<>(), selectedCount ->
                binding.btnFinish.setEnabled(selectedCount > 0));

        binding.rvMembers.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvMembers.setAdapter(adapter);

        // Load all server members
        ServerMemberRepository memberRepo = new ServerMemberRepository(requireContext());
        memberRepo.getServerMembers(serverId, result -> {
            if (result.isSuccess() && result.getData() != null) {
                adapter.updateList(result.getData());
            } else if (result.isError()) {
                Snackbar.make(view, result.getMessage(), Snackbar.LENGTH_SHORT).show();
            }
        });

        // Wire up search filtering
        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.filter(s.toString());
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Observe create result
        viewModel.createResult.observe(getViewLifecycleOwner(), result -> {
            if (result == null) return;
            if (result.isSuccess()) {
                viewModel.resetCreateResult();
                viewModel.resetRoles();
                // Pop all create role steps and navigate to roles list
                requireActivity().getSupportFragmentManager()
                        .popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE);
                ((ServerSettingsActivity) requireActivity()).navigateTo(
                        RolesListFragment.newInstance(serverId), false);
            } else if (result.isError()) {
                Snackbar.make(view, result.getMessage(), Snackbar.LENGTH_SHORT).show();
                viewModel.resetCreateResult();
            }
        });

        binding.btnFinish.setOnClickListener(v -> createRole());
        binding.btnSkip.setOnClickListener(v -> createRole());
    }

    private void createRole() {
        List<String> selectedIds = adapter != null ? new ArrayList<>(adapter.getSelectedIds()) : new ArrayList<>();
        viewModel.createRole(serverId, roleName, roleColor, preset,
                selectedIds.isEmpty() ? null : selectedIds);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
