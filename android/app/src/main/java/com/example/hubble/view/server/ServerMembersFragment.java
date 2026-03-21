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
import com.example.hubble.adapter.server.ServerMemberAdapter;
import com.example.hubble.data.model.AuthResult;
import com.example.hubble.data.model.server.ServerMemberItem;
import com.example.hubble.databinding.FragmentServerMembersBinding;
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
        adapter = new ServerMemberAdapter(member -> showMemberDetail(member));
        binding.rvMembers.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvMembers.setAdapter(adapter);

        // Toolbar
        binding.toolbar.setNavigationOnClickListener(v -> requireActivity().onBackPressed());

        // Observe members
        viewModel.getMembersState().observe(getViewLifecycleOwner(), result -> {
            if (result == null) return;

            if (result.getStatus() == AuthResult.Status.LOADING) {
                // Show loading state if needed
                return;
            }

            if (result.getStatus() == AuthResult.Status.SUCCESS && result.getData() != null) {
                allMembers = new ArrayList<>(result.getData());
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

        // Add member button
        binding.btnAddMember.setOnClickListener(v -> {
            Snackbar.make(binding.getRoot(), R.string.main_coming_soon, Snackbar.LENGTH_SHORT).show();
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
        // Add all members
        for (ServerMemberItem member : members) {
            items.add(new ServerMemberAdapter.AdapterItem(member));
        }
        adapter.submitList(items);
    }

    private void showMemberDetail(ServerMemberItem member) {
        MemberDetailBottomSheet sheet = MemberDetailBottomSheet.newInstance(member, serverId);
        sheet.show(getParentFragmentManager(), "MemberDetail");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
