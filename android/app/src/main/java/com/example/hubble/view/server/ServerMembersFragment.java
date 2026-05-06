package com.example.hubble.view.server;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.hubble.R;
import com.example.hubble.adapter.server.ServerMemberAdapter;
import com.example.hubble.data.model.auth.AuthResult;
import com.example.hubble.data.model.server.ServerMemberItem;
import com.example.hubble.databinding.FragmentServerMembersBinding;
import com.example.hubble.utils.TokenManager;
import com.example.hubble.viewmodel.server.ServerSettingsViewModel;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

public class ServerMembersFragment extends Fragment {
    private FragmentServerMembersBinding binding;
    private ServerSettingsViewModel viewModel;
    private ServerMemberAdapter adapter;
    private String serverId;
    private String serverName;
    private List<ServerMemberItem> allMembers = new ArrayList<>();
    private boolean isCurrentUserOwner = false;

    // Launcher: reload member list when MemberEditActivity returns RESULT_OK
    private final ActivityResultLauncher<Intent> memberEditLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && serverId != null) {
                    viewModel.loadMembers(serverId);
                }
            }
    );

    public static ServerMembersFragment newInstance(String serverId, String serverName) {
        ServerMembersFragment fragment = new ServerMembersFragment();
        Bundle args = new Bundle();
        args.putString("server_id", serverId);
        args.putString("server_name", serverName);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentServerMembersBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            serverId = getArguments().getString("server_id");
            serverName = getArguments().getString("server_name");
        }

        viewModel = new ViewModelProvider(requireActivity()).get(ServerSettingsViewModel.class);

        // Setup adapter
        adapter = new ServerMemberAdapter(member -> openMemberEdit(member));
        binding.rvMembers.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvMembers.setAdapter(adapter);

        // Toolbar
        binding.toolbar.setNavigationOnClickListener(v -> requireActivity().onBackPressed());

        // Observe members
        viewModel.getMembersState().observe(getViewLifecycleOwner(), result -> {
            if (result == null) return;

            if (result.getStatus() == AuthResult.Status.LOADING) {
                return;
            }

            if (result.getStatus() == AuthResult.Status.SUCCESS && result.getData() != null) {
                allMembers = new ArrayList<>(result.getData());

                // Determine if current user is owner
                TokenManager tokenManager = new TokenManager(requireContext());
                String currentUserId = tokenManager.getUser() != null ? tokenManager.getUser().getId() : "";
                isCurrentUserOwner = false;
                for (ServerMemberItem m : allMembers) {
                    if (m.getUserId().equals(currentUserId) && m.isOwner()) {
                        isCurrentUserOwner = true;
                        break;
                    }
                }

                submitToAdapter(allMembers);
                return;
            }

            if (result.getStatus() == AuthResult.Status.ERROR) {
                Snackbar.make(binding.getRoot(),
                        result.getMessage() != null ? result.getMessage() : getString(R.string.error_generic),
                        Snackbar.LENGTH_SHORT).show();
            }
        });

        // Search functionality
        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterMembers(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void filterMembers(String query) {
        List<ServerMemberItem> filtered = new ArrayList<>();
        String lowerQuery = query.toLowerCase().trim();

        for (ServerMemberItem member : allMembers) {
            boolean matchesQuery = member.getUsername().toLowerCase().contains(lowerQuery) ||
                    (member.getDisplayName() != null &&
                            member.getDisplayName().toLowerCase().contains(lowerQuery));
            if (matchesQuery) {
                filtered.add(member);
            }
        }

        submitToAdapter(filtered);
    }

    private void submitToAdapter(List<ServerMemberItem> members) {
        List<ServerMemberAdapter.AdapterItem> items = new ArrayList<>();
        // Add header
        items.add(new ServerMemberAdapter.AdapterItem(
                getString(R.string.members_section_label, members.size())
        ));
        // Owner first, then others
        for (ServerMemberItem member : members) {
            if (member.isOwner()) {
                items.add(1, new ServerMemberAdapter.AdapterItem(member));
            } else {
                items.add(new ServerMemberAdapter.AdapterItem(member));
            }
        }
        adapter.submitList(items);
    }

    private void openMemberEdit(ServerMemberItem member) {
        TokenManager tokenManager = new TokenManager(requireContext());
        String currentUserId = tokenManager.getUser() != null ? tokenManager.getUser().getId() : "";
        boolean isSelf = member.getUserId().equals(currentUserId);

        // Owner can manage any member except themselves
        boolean showOwnerActions = isCurrentUserOwner && !isSelf;

        Intent intent = new Intent(requireContext(), MemberEditActivity.class);
        intent.putExtra(MemberEditActivity.EXTRA_USER_ID, member.getUserId());
        intent.putExtra(MemberEditActivity.EXTRA_USERNAME, member.getUsername());
        intent.putExtra(MemberEditActivity.EXTRA_DISPLAY_NAME, member.getDisplayName());
        intent.putExtra(MemberEditActivity.EXTRA_AVATAR_URL, member.getAvatarUrl());
        intent.putExtra(MemberEditActivity.EXTRA_AVATAR_BG_COLOR, member.getAvatarBackgroundColor());
        intent.putExtra(MemberEditActivity.EXTRA_IS_CURRENT_USER_OWNER, showOwnerActions);
        intent.putExtra(MemberEditActivity.EXTRA_SERVER_ID, serverId);
        memberEditLauncher.launch(intent);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
