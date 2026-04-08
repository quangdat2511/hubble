package com.example.hubble.view.server;

import android.content.Context;
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
import com.example.hubble.adapter.server.RoleMemberAdapter;
import com.example.hubble.data.model.server.MemberBriefResponse;
import com.example.hubble.data.repository.RoleRepository;
import com.example.hubble.databinding.FragmentRoleMembersBinding;
import com.example.hubble.viewmodel.RolesViewModel;
import com.example.hubble.viewmodel.RolesViewModelFactory;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Shows members assigned to a role with option to remove them.
 */
public class RoleMembersFragment extends Fragment {

    private static final ConcurrentHashMap<String, List<MemberBriefResponse>> membersCache = new ConcurrentHashMap<>();

    private FragmentRoleMembersBinding binding;
    private RolesViewModel viewModel;
    private RoleMemberAdapter adapter;

    private String roleId;
    private String roleName;
    private String serverId;

    public static void prefetch(Context context, String serverId, String roleId) {
        if (membersCache.containsKey(roleId)) return;
        RoleRepository repo = new RoleRepository(context);
        repo.getMembers(serverId, roleId, result -> {
            if (result.isSuccess() && result.getData() != null) {
                membersCache.put(roleId, result.getData());
            }
        });
    }

    public static RoleMembersFragment newInstance(String roleId, String roleName, String serverId) {
        RoleMembersFragment fragment = new RoleMembersFragment();
        Bundle args = new Bundle();
        args.putString("role_id", roleId);
        args.putString("role_name", roleName);
        args.putString("server_id", serverId);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentRoleMembersBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            roleId = getArguments().getString("role_id");
            roleName = getArguments().getString("role_name");
            serverId = getArguments().getString("server_id");
        }

        viewModel = new ViewModelProvider(requireActivity(),
                new RolesViewModelFactory(new RoleRepository(requireContext())))
                .get(RolesViewModel.class);

        binding.toolbar.setTitle(roleName);
        binding.toolbar.setSubtitle(getString(R.string.server_settings_roles));
        binding.toolbar.setNavigationOnClickListener(v -> requireActivity().onBackPressed());

        adapter = new RoleMemberAdapter(new ArrayList<>());
        binding.rvMembers.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvMembers.setAdapter(adapter);

        // Clear stale data from previous role
        viewModel.resetMembers();

        // Serve from cache first
        List<MemberBriefResponse> cached = membersCache.get(roleId);
        if (cached != null) {
            adapter.updateList(cached);
            binding.tvEmpty.setVisibility(cached.isEmpty() ? View.VISIBLE : View.GONE);
            binding.rvMembers.setVisibility(cached.isEmpty() ? View.GONE : View.VISIBLE);
        }

        viewModel.members.observe(getViewLifecycleOwner(), result -> {
            if (result == null) return;
            if (result.isSuccess() && result.getData() != null) {
                List<MemberBriefResponse> members = result.getData();
                membersCache.put(roleId, members);
                adapter.updateList(members);
                binding.tvEmpty.setVisibility(members.isEmpty() ? View.VISIBLE : View.GONE);
                binding.rvMembers.setVisibility(members.isEmpty() ? View.GONE : View.VISIBLE);
            } else if (result.isError() && cached == null) {
                Snackbar.make(view, result.getMessage(), Snackbar.LENGTH_SHORT).show();
            }
        });

        // Always refresh from API
        viewModel.loadMembers(serverId, roleId);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
