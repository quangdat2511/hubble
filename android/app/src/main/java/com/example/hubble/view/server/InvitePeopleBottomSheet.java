package com.example.hubble.view.server;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.hubble.R;
import com.example.hubble.adapter.server.ServerInviteAdapter;
import com.example.hubble.data.repository.ServerInviteRepository;
import com.example.hubble.databinding.BottomSheetInvitePeopleBinding;
import com.example.hubble.viewmodel.server.ServerInviteViewModel;
import com.example.hubble.viewmodel.server.ServerInviteViewModelFactory;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.snackbar.Snackbar;

public class InvitePeopleBottomSheet extends BottomSheetDialogFragment {

    private BottomSheetInvitePeopleBinding binding;
    private ServerInviteViewModel viewModel;
    private ServerInviteAdapter adapter;
    private String serverId;
    private String serverName;

    public static InvitePeopleBottomSheet newInstance(String serverId, String serverName) {
        InvitePeopleBottomSheet sheet = new InvitePeopleBottomSheet();
        Bundle args = new Bundle();
        args.putString("server_id", serverId);
        args.putString("server_name", serverName);
        sheet.setArguments(args);
        return sheet;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = BottomSheetInvitePeopleBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            serverId = getArguments().getString("server_id");
            serverName = getArguments().getString("server_name");
        }

        binding.tvServerName.setText(
                getString(R.string.invite_to_server, serverName != null ? serverName : ""));

        // ViewModel
        ServerInviteRepository repo = new ServerInviteRepository(requireContext());
        viewModel = new ViewModelProvider(this,
                new ServerInviteViewModelFactory(repo))
                .get(ServerInviteViewModel.class);

        // Recent invites RecyclerView
        adapter = new ServerInviteAdapter();
        binding.rvRecentInvites.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvRecentInvites.setAdapter(adapter);

        // Send button — enabled only when input is non-empty
        binding.btnSend.setEnabled(false);
        binding.etUsername.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                binding.btnSend.setEnabled(!TextUtils.isEmpty(s.toString().trim()));
            }
        });

        binding.etUsername.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendInvite();
                return true;
            }
            return false;
        });

        binding.btnSend.setOnClickListener(v -> sendInvite());

        // Observe create result
        viewModel.getCreateInviteState().observe(getViewLifecycleOwner(), result -> {
            if (result == null) return;
            binding.btnSend.setEnabled(true);
            if (result.isLoading()) {
                binding.btnSend.setEnabled(false);
                return;
            }
            if (result.isSuccess()) {
                binding.etUsername.setText("");
                binding.tilUsername.setError(null);
                Snackbar.make(binding.getRoot(),
                        R.string.invite_sent_success, Snackbar.LENGTH_SHORT).show();
                viewModel.consumeCreateInviteState();
            } else if (result.isError()) {
                binding.tilUsername.setError(result.getMessage());
                viewModel.consumeCreateInviteState();
            }
        });

        // Observe recent invites (server invite list)
        viewModel.getServerInvitesState().observe(getViewLifecycleOwner(), result -> {
            if (result == null) return;
            binding.progressBar.setVisibility(result.isLoading() ? View.VISIBLE : View.GONE);

            if (result.isSuccess()) {
                boolean empty = result.getData() == null || result.getData().isEmpty();
                binding.tvRecentHeader.setVisibility(empty ? View.GONE : View.VISIBLE);
                binding.rvRecentInvites.setVisibility(empty ? View.GONE : View.VISIBLE);
                binding.layoutEmptyRecent.setVisibility(empty ? View.VISIBLE : View.GONE);
                if (!empty) adapter.submitList(result.getData());
            } else if (result.isError()) {
                binding.tvRecentHeader.setVisibility(View.GONE);
                binding.rvRecentInvites.setVisibility(View.GONE);
                binding.layoutEmptyRecent.setVisibility(View.VISIBLE);
            }
        });

        // Load recent invites
        binding.progressBar.setVisibility(View.VISIBLE);
        viewModel.loadServerInvites(serverId);
    }

    private void sendInvite() {
        String username = binding.etUsername.getText() != null
                ? binding.etUsername.getText().toString().trim() : "";
        if (TextUtils.isEmpty(username)) {
            binding.tilUsername.setError(getString(R.string.invite_error_empty_username));
            return;
        }
        binding.tilUsername.setError(null);
        viewModel.createInvite(username);
    }

    @Override
    public void onStart() {
        super.onStart();
        BottomSheetDialog dialog = (BottomSheetDialog) getDialog();
        if (dialog != null) {
            BottomSheetBehavior<?> behavior = dialog.getBehavior();
            behavior.setPeekHeight(BottomSheetBehavior.PEEK_HEIGHT_AUTO);
            behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            behavior.setSkipCollapsed(true);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

