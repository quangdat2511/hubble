package com.example.hubble.view;

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
import com.example.hubble.adapter.DmConversationAdapter;
import com.example.hubble.adapter.DmStoryAdapter;
import com.example.hubble.databinding.FragmentHomeBinding;
import com.example.hubble.adapter.ServerSidebarAdapter;
import com.example.hubble.data.model.AuthResult;
import com.example.hubble.data.repository.DmRepository;
import com.example.hubble.data.repository.ServerRepository;
import com.example.hubble.view.server.CreateServerActivity;
import com.example.hubble.viewmodel.MainViewModel;
import com.example.hubble.viewmodel.MainViewModelFactory;
import com.google.android.material.snackbar.Snackbar;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private ServerSidebarAdapter serverAdapter;
    private DmStoryAdapter storyAdapter;
    private DmConversationAdapter conversationAdapter;
    private DmRepository dmRepository;
    private ServerRepository serverRepository;
    private MainViewModel viewModel;

    private final ActivityResultLauncher<Intent> createServerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == android.app.Activity.RESULT_OK) {
                    loadServers();
                    if (binding != null) {
                        Snackbar.make(binding.getRoot(),
                                getString(R.string.create_server_success),
                                Snackbar.LENGTH_SHORT).show();
                    }
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

        dmRepository = new DmRepository(requireContext());
        serverRepository = new ServerRepository(requireContext());

        viewModel = new ViewModelProvider(
            requireActivity(),
            new MainViewModelFactory(dmRepository)
        ).get(MainViewModel.class);

        setupServerSidebar(viewModel);
        setupStories(viewModel);
        setupConversations(viewModel);
        setupActions(view);
        loadServers();
        viewModel.refreshDirectMessages();
    }

    private void loadServers() {
        serverRepository.getMyServers(result -> {
            if (result.getStatus() == AuthResult.Status.SUCCESS && result.getData() != null) {
                viewModel.setServers(result.getData());
                return;
            }

            if (result.getStatus() == AuthResult.Status.ERROR && binding != null) {
                String message = result.getMessage() != null
                        ? result.getMessage()
                        : getString(R.string.error_generic);
                Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_SHORT).show();
            }
        });
    }

    private void setupServerSidebar(MainViewModel viewModel) {
        serverAdapter = new ServerSidebarAdapter();
        binding.rvServers.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvServers.setAdapter(serverAdapter);

        viewModel.servers.observe(getViewLifecycleOwner(), servers -> {
            if (servers != null) {
                serverAdapter.setServers(servers);
            }
        });

        serverAdapter.setOnServerClickListener((server, position) -> viewModel.selectServer(server));

        binding.fabAddServer.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), CreateServerActivity.class);
            createServerLauncher.launch(intent);
        });
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
                startActivity(DmChatActivity.createIntent(requireContext(), item.getChannelId(), item.getDisplayName()));
                return;
            }

            String friendId = item.getFriendId();
            if (friendId == null || friendId.trim().isEmpty()) {
                if (binding != null) {
                    Snackbar.make(binding.getRoot(), getString(R.string.error_generic), Snackbar.LENGTH_SHORT).show();
                }
                return;
            }

            dmRepository.getOrCreateDirectChannel(friendId, result -> {
                if (result.getStatus() == AuthResult.Status.SUCCESS && result.getData() != null) {
                    startActivity(DmChatActivity.createIntent(
                            requireContext(),
                            result.getData().getId(),
                            item.getDisplayName()
                    ));
                    return;
                }
                String error = result.getMessage() != null ? result.getMessage() : getString(R.string.error_generic);
                if (binding != null) {
                    Snackbar.make(binding.getRoot(), error, Snackbar.LENGTH_SHORT).show();
                }
            });
        });

        viewModel.errorMessage.observe(getViewLifecycleOwner(), message -> {
            if (message != null && !message.trim().isEmpty()) {
                if (binding != null) {
                    Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_SHORT).show();
                }
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
