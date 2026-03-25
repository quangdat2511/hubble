package com.example.hubble.view;

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
import com.example.hubble.adapter.server.PendingInviteAdapter;
import com.example.hubble.data.model.AuthResult;
import com.example.hubble.data.model.server.ServerInviteResponse;
import com.example.hubble.data.repository.DmRepository;
import com.example.hubble.data.repository.ServerInviteRepository;
import com.example.hubble.data.repository.ServerRepository;
import com.example.hubble.databinding.FragmentNotificationsBinding;
import com.example.hubble.viewmodel.home.MainViewModel;
import com.example.hubble.viewmodel.home.MainViewModelFactory;
import com.example.hubble.viewmodel.server.ServerInviteViewModel;
import com.example.hubble.viewmodel.server.ServerInviteViewModelFactory;
import com.google.android.material.snackbar.Snackbar;

public class NotificationsFragment extends Fragment {

    private FragmentNotificationsBinding binding;
    private ServerInviteViewModel viewModel;
    private PendingInviteAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentNotificationsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // ViewModel scoped to this fragment
        ServerInviteRepository repo = new ServerInviteRepository(requireContext());
        viewModel = new ViewModelProvider(this,
                new ServerInviteViewModelFactory(repo))
                .get(ServerInviteViewModel.class);

        // MainViewModel scoped to the activity — used to refresh the server list
        MainViewModel mainViewModel = new ViewModelProvider(requireActivity(),
                new MainViewModelFactory(
                        new DmRepository(requireContext()),
                        new ServerRepository(requireContext())))
                .get(MainViewModel.class);

        // More options button (existing)
        binding.btnNotificationsMore.setOnClickListener(v ->
                Snackbar.make(view, getString(R.string.main_coming_soon),
                        Snackbar.LENGTH_SHORT).show());

        // Pending invites RecyclerView
        adapter = new PendingInviteAdapter(new PendingInviteAdapter.OnInviteActionListener() {
            @Override
            public void onAccept(ServerInviteResponse invite) {
                viewModel.acceptInvite(invite.getId());
            }

            @Override
            public void onDecline(ServerInviteResponse invite) {
                viewModel.declineInvite(invite.getId());
            }
        });
        binding.rvPendingInvites.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvPendingInvites.setAdapter(adapter);

        // Observe pending invites
        viewModel.getMyInvitesState().observe(getViewLifecycleOwner(), result -> {
            if (result == null) return;
            binding.progressBar.setVisibility(result.isLoading() ? View.VISIBLE : View.GONE);

            if (result.isSuccess()) {
                boolean empty = result.getData() == null || result.getData().isEmpty();
                binding.layoutEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
                binding.tvInvitesSectionLabel.setVisibility(empty ? View.GONE : View.VISIBLE);
                binding.rvPendingInvites.setVisibility(empty ? View.GONE : View.VISIBLE);
                if (!empty) adapter.submitList(result.getData());
            } else if (result.isError()) {
                binding.layoutEmpty.setVisibility(View.VISIBLE);
                binding.tvInvitesSectionLabel.setVisibility(View.GONE);
                binding.rvPendingInvites.setVisibility(View.GONE);
            }
        });

        // Observe accept/decline result
        viewModel.getRespondState().observe(getViewLifecycleOwner(), result -> {
            if (result == null) return;
            if (result.isSuccess()) {
                Snackbar.make(binding.getRoot(),
                        getString(R.string.invite_respond_success), Snackbar.LENGTH_SHORT).show();
                viewModel.consumeRespondState();
            } else if (result.isError()) {
                Snackbar.make(binding.getRoot(),
                        result.getMessage() != null ? result.getMessage()
                                : getString(R.string.error_generic),
                        Snackbar.LENGTH_SHORT).show();
                viewModel.consumeRespondState();
            }
        });

        // When an invite is accepted → reload the server list visible in HomeFragment
        viewModel.getAcceptSuccessEvent().observe(getViewLifecycleOwner(), accepted -> {
            if (accepted == null || !accepted) return;
            mainViewModel.refreshServers();
            viewModel.consumeAcceptSuccessEvent();
        });

        // Initial load (show loading while we wait)
        binding.progressBar.setVisibility(View.VISIBLE);
        viewModel.loadMyInvites();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
