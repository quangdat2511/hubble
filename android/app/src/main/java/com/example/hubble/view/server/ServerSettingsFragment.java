package com.example.hubble.view.server;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.hubble.R;
import com.example.hubble.data.model.AuthResult;
import com.example.hubble.data.repository.ServerMemberRepository;
import com.example.hubble.databinding.FragmentServerSettingsBinding;
import com.example.hubble.viewmodel.server.ServerSettingsViewModel;
import com.example.hubble.viewmodel.server.ServerSettingsViewModelFactory;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

public class ServerSettingsFragment extends Fragment {
    private FragmentServerSettingsBinding binding;
    private ServerSettingsViewModel viewModel;
    private String serverId;
    private String serverName;

    public static ServerSettingsFragment newInstance(String serverId, String serverName) {
        ServerSettingsFragment fragment = new ServerSettingsFragment();
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
        binding = FragmentServerSettingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            serverId = getArguments().getString("server_id");
            serverName = getArguments().getString("server_name");
        }

        ServerMemberRepository repository = new ServerMemberRepository(requireContext());
        viewModel = new ViewModelProvider(
                requireActivity(),
                new ServerSettingsViewModelFactory(repository)
        ).get(ServerSettingsViewModel.class);

        viewModel.loadMembers(serverId);

        // Toolbar
        binding.toolbar.setNavigationOnClickListener(v -> requireActivity().onBackPressed());

        // Members count
        viewModel.getMembersState().observe(getViewLifecycleOwner(), result -> {
            if (result != null && result.getStatus() == AuthResult.Status.SUCCESS) {
                if (result.getData() != null) {
                    binding.tvMemberCount.setText(String.valueOf(result.getData().size()));
                }
            }
        });

        // Row click listeners
        binding.rowServerName.setOnClickListener(v -> showComingSoon());
        binding.rowServerIcon.setOnClickListener(v -> showComingSoon());
        binding.rowServerDescription.setOnClickListener(v -> showComingSoon());

        binding.rowMembers.setOnClickListener(v -> {
            if (serverId != null) {
                ((ServerSettingsActivity) requireActivity()).navigateTo(
                        ServerMembersFragment.newInstance(serverId, serverName), true);
            }
        });

        binding.rowBans.setOnClickListener(v -> showComingSoon());
        binding.rowInvites.setOnClickListener(v -> {
            if (serverId != null) {
                ((ServerSettingsActivity) requireActivity()).navigateTo(
                        ServerInviteFragment.newInstance(serverId, serverName), true);
            }
        });
        binding.rowRoles.setOnClickListener(v -> showComingSoon());
        binding.rowEmoji.setOnClickListener(v -> showComingSoon());
        binding.rowStickers.setOnClickListener(v -> showComingSoon());

        binding.cardDeleteServer.setOnClickListener(v -> {
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.server_settings_delete_confirm_title)
                    .setMessage(R.string.server_settings_delete_confirm_message)
                    .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                        showComingSoon();
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void showComingSoon() {
        Snackbar.make(binding.getRoot(), R.string.main_coming_soon, Snackbar.LENGTH_SHORT).show();
    }
}
