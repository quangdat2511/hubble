package com.example.hubble.view.server;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.hubble.R;
import com.example.hubble.adapter.server.ServerInviteAdapter;
import com.example.hubble.data.model.AuthResult;
import com.example.hubble.data.repository.ServerInviteRepository;
import com.example.hubble.databinding.FragmentServerInvitesBinding;
import com.example.hubble.viewmodel.server.ServerInviteViewModel;
import com.example.hubble.viewmodel.server.ServerInviteViewModelFactory;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class ServerInviteFragment extends Fragment {

    private FragmentServerInvitesBinding binding;
    private ServerInviteViewModel viewModel;
    private ServerInviteAdapter adapter;
    private String serverId;
    private String serverName;

    public static ServerInviteFragment newInstance(String serverId, String serverName) {
        ServerInviteFragment fragment = new ServerInviteFragment();
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
        binding = FragmentServerInvitesBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            serverId = getArguments().getString("server_id");
            serverName = getArguments().getString("server_name");
        }

        // ViewModel
        ServerInviteRepository repo = new ServerInviteRepository(requireContext());
        viewModel = new ViewModelProvider(this,
                new ServerInviteViewModelFactory(repo))
                .get(ServerInviteViewModel.class);

        // Toolbar
        binding.toolbar.setNavigationOnClickListener(v -> requireActivity().onBackPressed());

        // RecyclerView
        adapter = new ServerInviteAdapter();
        binding.rvInvites.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvInvites.setAdapter(adapter);

        // FAB
        binding.fabInvite.setOnClickListener(v -> showCreateInviteDialog());

        // Observe server invites list
        viewModel.getServerInvitesState().observe(getViewLifecycleOwner(), result -> {
            if (result == null) return;
            binding.progressBar.setVisibility(
                    result.isLoading() ? View.VISIBLE : View.GONE);

            if (result.isSuccess()) {
                boolean empty = result.getData() == null || result.getData().isEmpty();
                binding.layoutEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
                binding.rvInvites.setVisibility(empty ? View.GONE : View.VISIBLE);
                if (!empty) adapter.submitList(result.getData());
            } else if (result.isError()) {
                binding.layoutEmpty.setVisibility(View.VISIBLE);
                binding.rvInvites.setVisibility(View.GONE);
                showSnackbar(result.getMessage());
            }
        });

        // Observe create invite result
        viewModel.getCreateInviteState().observe(getViewLifecycleOwner(), result -> {
            if (result == null) return;
            if (result.isSuccess()) {
                showSnackbar(getString(R.string.invite_sent_success));
                viewModel.consumeCreateInviteState();
            } else if (result.isError()) {
                showSnackbar(result.getMessage());
                viewModel.consumeCreateInviteState();
            }
        });

        // Load invites
        viewModel.loadServerInvites(serverId);
    }

    private void showCreateInviteDialog() {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_invite_user, null);
        TextInputLayout tilUsername = dialogView.findViewById(R.id.tilInviteeUsername);
        TextInputEditText etUsername = dialogView.findViewById(R.id.etInviteeUsername);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.invite_create_title)
                .setView(dialogView)
                .setPositiveButton(R.string.invite_send_btn, (dialog, which) -> {
                    String username = etUsername.getText() != null
                            ? etUsername.getText().toString().trim() : "";
                    if (TextUtils.isEmpty(username)) {
                        tilUsername.setError(getString(R.string.invite_error_empty_username));
                        return;
                    }
                    tilUsername.setError(null);
                    viewModel.createInvite(username);
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void showSnackbar(String message) {
        if (message == null || binding == null) return;
        Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

