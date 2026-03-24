package com.example.hubble.view.home;

import android.content.Intent;
import android.os.Bundle;
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
import com.example.hubble.adapter.dm.DmConversationAdapter;
import com.example.hubble.adapter.dm.DmStoryAdapter;
import com.example.hubble.adapter.home.ServerSidebarAdapter;
import com.example.hubble.databinding.FragmentHomeBinding;
import com.example.hubble.data.model.AuthResult;
import com.example.hubble.data.model.server.ServerItem;
import com.example.hubble.data.repository.DmRepository;
import com.example.hubble.data.repository.ServerRepository;
//import com.example.hubble.view.dm.DmChatActivity;
import com.example.hubble.view.dm.NewMessageActivity;
import com.example.hubble.view.server.CreateServerActivity;
import com.example.hubble.viewmodel.home.MainViewModel;
import com.example.hubble.viewmodel.home.MainViewModelFactory;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private ServerSidebarAdapter serverAdapter;
    private DmStoryAdapter storyAdapter;
    private DmConversationAdapter conversationAdapter;
    private MainViewModel viewModel;
    private final List<ServerItem> currentServers = new ArrayList<>();
    private String pendingDmDisplayName;

    private final ActivityResultLauncher<Intent> createServerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == android.app.Activity.RESULT_OK) {
                    viewModel.refreshServers();
                    showMessage(getString(R.string.create_server_success));
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(
            requireActivity(),
            new MainViewModelFactory(new DmRepository(requireContext()), new ServerRepository(requireContext()))
        ).get(MainViewModel.class);

        setupServerSidebar(viewModel);
        setupStories(viewModel);
        setupConversations(viewModel);
        setupActions(view);
        viewModel.refreshDirectMessages();
    }

    private void setupServerSidebar(MainViewModel viewModel) {
        serverAdapter = new ServerSidebarAdapter();
        binding.rvServers.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvServers.setAdapter(serverAdapter);

        viewModel.servers.observe(getViewLifecycleOwner(), servers -> {
            if (servers != null) {
                currentServers.clear();
                currentServers.addAll(servers);
                serverAdapter.setServers(servers);
                syncSelectedServer(viewModel.selectedServer.getValue());
            }
        });

        viewModel.selectedServer.observe(getViewLifecycleOwner(), this::syncSelectedServer);

        serverAdapter.setOnServerClickListener((server, position) -> viewModel.selectServer(server));

        binding.fabAddServer.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), CreateServerActivity.class);
            createServerLauncher.launch(intent);
        });
    }

    private void syncSelectedServer(@Nullable ServerItem selectedServer) {
        if (selectedServer == null || currentServers.isEmpty()) {
            return;
        }

        for (int i = 0; i < currentServers.size(); i++) {
            ServerItem item = currentServers.get(i);
            if (item.getId() != null && item.getId().equals(selectedServer.getId())) {
                serverAdapter.setSelectedPosition(i);
                return;
            }
        }
    }

    private void setupStories(MainViewModel viewModel) {
        storyAdapter = new DmStoryAdapter();
        binding.rvStories.setLayoutManager(
                new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        binding.rvStories.setAdapter(storyAdapter);

        viewModel.dmStories.observe(getViewLifecycleOwner(), stories -> {
            if (stories != null) {
                storyAdapter.setItems(stories);
            }
        });
    }

    private void setupConversations(MainViewModel viewModel) {
        conversationAdapter = new DmConversationAdapter();
        binding.rvConversations.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvConversations.setAdapter(conversationAdapter);

        viewModel.dmConversations.observe(getViewLifecycleOwner(), conversations -> {
            if (conversations != null) {
                conversationAdapter.setItems(conversations);
            }
        });

        conversationAdapter.setOnConversationClickListener(item -> {
            if (item.hasChannelId()) {
//                startActivity(DmChatActivity.createIntent(requireContext(), item.getChannelId(), item.getDisplayName()));
                return;
            }

            String friendId = item.getFriendId();
            if (friendId == null || friendId.trim().isEmpty()) {
                showMessage(getString(R.string.error_generic));
                return;
            }
            pendingDmDisplayName = item.getDisplayName();
            viewModel.openOrCreateDirectChannel(friendId);
        });

        viewModel.openDmState.observe(getViewLifecycleOwner(), result -> {
            if (result == null || result.getStatus() == AuthResult.Status.LOADING) {
                return;
            }

            if (result.getStatus() == AuthResult.Status.SUCCESS && result.getData() != null) {
//                startActivity(DmChatActivity.createIntent(
//                        requireContext(),
//                        result.getData().getId(),
//                        pendingDmDisplayName != null ? pendingDmDisplayName : getString(R.string.dm_default_user)
//                ));
                pendingDmDisplayName = null;
                viewModel.consumeOpenDmState();
                return;
            }

            String error = result.getMessage() != null ? result.getMessage() : getString(R.string.error_generic);
            showMessage(error);
            pendingDmDisplayName = null;
            viewModel.consumeOpenDmState();
        });

        viewModel.errorMessage.observe(getViewLifecycleOwner(), message -> {
            if (message != null && !message.trim().isEmpty()) {
                showMessage(message);
            }
        });
    }

    private void setupActions(View view) {
        binding.btnSearch.setOnClickListener(v ->
                Snackbar.make(view, getString(R.string.main_coming_soon), Snackbar.LENGTH_SHORT).show());

        binding.btnAddFriend.setOnClickListener(v ->
                Snackbar.make(view, getString(R.string.main_coming_soon), Snackbar.LENGTH_SHORT).show());

        binding.fabNewDm.setOnClickListener(v ->
            startActivity(NewMessageActivity.createIntent(requireContext())));
    }

    private void showMessage(String message) {
        if (binding == null || message == null || message.trim().isEmpty()) {
            return;
        }
        Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}



