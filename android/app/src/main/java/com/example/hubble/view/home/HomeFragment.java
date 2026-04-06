package com.example.hubble.view.home;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.example.hubble.R;
import com.example.hubble.adapter.dm.DmConversationAdapter;
import com.example.hubble.adapter.dm.DmStoryAdapter;
import com.example.hubble.adapter.home.ServerSidebarAdapter;
import com.example.hubble.data.api.NetworkConfig;
import com.example.hubble.adapter.server.ServerChannelAdapter;
import com.example.hubble.data.model.dm.DmConversationItem;
import com.example.hubble.data.model.dm.ChannelDto;
import com.example.hubble.databinding.BottomSheetDmConversationActionsBinding;
import com.example.hubble.databinding.FragmentHomeBinding;
import com.example.hubble.data.model.auth.AuthResult;
import com.example.hubble.data.model.dm.DmConversationItem;
import com.example.hubble.data.model.server.ServerItem;
import com.example.hubble.data.repository.DmRepository;
import com.example.hubble.data.repository.ServerRepository;
import com.example.hubble.databinding.BottomSheetDmConversationActionsBinding;
import com.example.hubble.databinding.FragmentHomeBinding;
import com.example.hubble.utils.AvatarPlaceholderUtils;
import com.example.hubble.view.dm.DmChatActivity;
import com.example.hubble.view.dm.NewMessageActivity;
import com.example.hubble.view.server.CategoryProfileBottomSheet;
import com.example.hubble.view.server.ChannelProfileBottomSheet;
import com.example.hubble.view.server.CreateServerActivity;
import com.example.hubble.view.server.ServerProfileBottomSheet;
import com.example.hubble.viewmodel.home.MainViewModel;
import com.example.hubble.viewmodel.home.MainViewModelFactory;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private ServerSidebarAdapter serverAdapter;
    private DmStoryAdapter storyAdapter;
    private DmConversationAdapter conversationAdapter;
    private ServerChannelAdapter serverChannelAdapter;
    private MainViewModel viewModel;
    private final List<ServerItem> currentServers = new ArrayList<>();
    private String pendingDmDisplayName;
    private String pendingDmAvatarUrl;

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
                new MainViewModelFactory(requireContext(), new DmRepository(requireContext()), new ServerRepository(requireContext()))
        ).get(MainViewModel.class);

        setupServerSidebar(viewModel);
        setupServerChannels(viewModel);
        setupStories(viewModel);
        setupConversations(viewModel);
        setupActions(view);
        viewModel.refreshDirectMessages();

        viewModel.kickedFromServer.observe(getViewLifecycleOwner(), serverName -> {
            if (serverName != null) {
                Snackbar.make(requireView(),
                        "Bạn đã bị xóa khỏi \"" + serverName + "\"",
                        Snackbar.LENGTH_LONG).show();
                viewModel.consumeKickedFromServer();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (viewModel != null) {
            viewModel.refreshDirectMessages();
            // Refresh channels if a server is selected (e.g. after creating a channel)
            ServerItem server = viewModel.selectedServer.getValue();
            if (server != null) {
                viewModel.loadServerChannels(server.getId());
            }
        }
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

        viewModel.serverUnreadByServerId.observe(getViewLifecycleOwner(), map -> {
            if (serverAdapter != null) {
                serverAdapter.setUnreadByServerId(map);
            }
        });

        viewModel.dmTotalUnread.observe(getViewLifecycleOwner(), n -> syncDmSidebarBadges());

        viewModel.selectedServer.observe(getViewLifecycleOwner(), selectedServer -> {
            syncSelectedServer(selectedServer);
            updateDmButtonState(selectedServer == null);
        });

        serverAdapter.setOnServerClickListener((server, position) -> viewModel.selectServer(server));

        binding.btnDmView.setOnClickListener(v -> viewModel.selectDmPanel());

        binding.fabAddServer.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), CreateServerActivity.class);
            createServerLauncher.launch(intent);
        });
    }

    private void updateDmButtonState(boolean isActive) {
        binding.viewDmActiveIndicator.setVisibility(isActive ? View.VISIBLE : View.GONE);

        int color = isActive
                ? ContextCompat.getColor(requireContext(), R.color.color_primary)
                : ContextCompat.getColor(requireContext(), R.color.color_text_secondary);
        binding.btnDmView.setIconTint(ColorStateList.valueOf(color));

        if (isActive) {
            serverAdapter.setSelectedPosition(-1);
        }
        syncDmSidebarBadges();
    }

    /** Discord-style: red count on DM icon + white left pill when unread and a server is selected. */
    private void syncDmSidebarBadges() {
        if (binding == null || viewModel == null) return;
        Integer total = viewModel.dmTotalUnread.getValue();
        int t = total != null ? total : 0;
        boolean dmPanel = viewModel.selectedServer.getValue() == null;
        binding.viewDmUnreadPill.setVisibility(t > 0 && !dmPanel ? View.VISIBLE : View.GONE);
        if (t > 0 && !dmPanel) {
            binding.tvDmSidebarBadge.setVisibility(View.VISIBLE);
            binding.tvDmSidebarBadge.setText(t > 99 ? "99+" : String.valueOf(t));
        } else {
            binding.tvDmSidebarBadge.setVisibility(View.GONE);
        }
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

    private void setupServerChannels(MainViewModel viewModel) {
        serverChannelAdapter = new ServerChannelAdapter(
            channel -> {
                if (Boolean.TRUE.equals(channel.getIsPrivate()) && !Boolean.TRUE.equals(channel.getCanAccess())) {
                    showMessage("Bạn không có quyền truy cập kênh " + channel.getName());
                } else {
                    showMessage("Kênh: " + channel.getName() + " (chức năng đang phát triển)");
                }
            },
            viewModel::toggleCategoryCollapse,
            channel -> {
                ServerItem server = viewModel.selectedServer.getValue();
                if (server != null) {
                    // Resolve parent category name
                    String parentName = null;
                    if (channel.getParentId() != null && viewModel.serverChannels.getValue() != null
                            && viewModel.serverChannels.getValue().getData() != null) {
                        for (ChannelDto ch : viewModel.serverChannels.getValue().getData()) {
                            if (channel.getParentId().equals(ch.getId())) {
                                parentName = ch.getName();
                                break;
                            }
                        }
                    }
                    ChannelProfileBottomSheet.newInstance(
                            server.getId(), server.getName(), server.getIconUrl(),
                            server.getOwnerId(),
                            channel.getId(), channel.getName(), channel.getType(),
                            channel.getTopic(), channel.getParentId(), parentName,
                            Boolean.TRUE.equals(channel.getIsPrivate())
                    ).show(getParentFragmentManager(), "ChannelProfile");
                }
            },
            category -> {
                ServerItem server = viewModel.selectedServer.getValue();
                if (server != null) {
                    CategoryProfileBottomSheet.newInstance(
                            server.getId(), server.getName(), server.getIconUrl(),
                            server.getOwnerId(),
                            category.getId(), category.getName(),
                            Boolean.TRUE.equals(category.getIsPrivate())
                    ).show(getParentFragmentManager(), "CategoryProfile");
                }
            }
        );

        binding.rvServerChannels.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvServerChannels.setAdapter(serverChannelAdapter);

        viewModel.selectedServer.observe(getViewLifecycleOwner(), server -> {
            if (server != null) {
                binding.layoutDmPanel.setVisibility(View.GONE);
                binding.layoutServerPanel.setVisibility(View.VISIBLE);
                binding.tvServerName.setText(server.getName());
                viewModel.loadServerChannels(server.getId());
            } else {
                binding.layoutServerPanel.setVisibility(View.GONE);
                binding.layoutDmPanel.setVisibility(View.VISIBLE);
            }
        });

        binding.layoutServerHeader.setOnClickListener(v -> {
            ServerItem server = viewModel.selectedServer.getValue();
            if (server != null) {
                ServerProfileBottomSheet sheet = ServerProfileBottomSheet.newInstance(server, 0, 0);
                sheet.show(getParentFragmentManager(), "ServerProfile");
            }
        });

        viewModel.serverChannels.observe(getViewLifecycleOwner(), result -> {
            if (result == null) return;
            if (result.getStatus() == AuthResult.Status.SUCCESS && result.getData() != null) {
                serverChannelAdapter.submitChannels(result.getData(), viewModel.getCollapsedCategories());
                return;
            }

            if (result.getStatus() == AuthResult.Status.ERROR) {
                String error = result.getMessage() != null ? result.getMessage() : getString(R.string.error_generic);
                showMessage(error);
            }
        });
    }

    private void setupStories(MainViewModel viewModel) {
        storyAdapter = new DmStoryAdapter();
        binding.rvStories.setLayoutManager(
                new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        binding.rvStories.setAdapter(storyAdapter);
        storyAdapter.setOnStoryClickListener(this::openConversation);

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

        conversationAdapter.setOnConversationClickListener(this::openConversation);
        conversationAdapter.setOnConversationLongClickListener(this::showConversationActionsSheet);

        viewModel.openDmState.observe(getViewLifecycleOwner(), result -> {
            if (result == null || result.getStatus() == AuthResult.Status.LOADING) {
                return;
            }

            if (result.getStatus() == AuthResult.Status.SUCCESS && result.getData() != null) {
                openDmChat(
                        result.getData().getId(),
                        pendingDmDisplayName != null ? pendingDmDisplayName : getString(R.string.dm_default_user),
                        firstNonBlank(pendingDmAvatarUrl, result.getData().getPeerAvatarUrl())
                );
                pendingDmDisplayName = null;
                pendingDmAvatarUrl = null;
                viewModel.consumeOpenDmState();
                return;
            }

            String error = result.getMessage() != null ? result.getMessage() : getString(R.string.error_generic);
            showMessage(error);
            pendingDmDisplayName = null;
            pendingDmAvatarUrl = null;
            viewModel.consumeOpenDmState();
        });

        viewModel.errorMessage.observe(getViewLifecycleOwner(), message -> {
            if (message != null && !message.trim().isEmpty()) {
                showMessage(message);
            }
        });
    }

    private void openConversation(@Nullable DmConversationItem item) {
        if (item == null) {
            showMessage(getString(R.string.error_generic));
            return;
        }

        String displayName = item.getDisplayName() != null && !item.getDisplayName().trim().isEmpty()
                ? item.getDisplayName()
                : getString(R.string.dm_default_user);

        if (item.hasChannelId()) {
            startActivity(DmChatActivity.createIntent(
                    requireContext(),
                    item.getChannelId(),
                    displayName,
                    item.getAvatarUrl()
            ));
            return;
        }

        String friendId = item.getFriendId();
        if (friendId == null || friendId.trim().isEmpty()) {
            showMessage(getString(R.string.error_generic));
            return;
        }

        pendingDmDisplayName = displayName;
        pendingDmAvatarUrl = item.getAvatarUrl();
        viewModel.openOrCreateDirectChannel(friendId);
    }

    private void showConversationActionsSheet(DmConversationItem item) {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        BottomSheetDmConversationActionsBinding sheet = BottomSheetDmConversationActionsBinding.inflate(getLayoutInflater());
        dialog.setContentView(sheet.getRoot());

        String displayName = item != null && item.getDisplayName() != null ? item.getDisplayName().trim() : "";
        if (displayName.isEmpty()) {
            displayName = getString(R.string.dm_default_user);
        }
        sheet.tvConversationHandle.setText("@" + displayName);
        bindConversationAvatar(
                sheet.ivConversationAvatar,
                item != null ? item.getAvatarUrl() : null,
                displayName
        );

        boolean isFavorite = item != null && item.isFavorite();
        sheet.actionFavorite.setText(isFavorite ? R.string.dm_unfavorite : R.string.dm_favorite);

        sheet.actionProfile.setOnClickListener(v -> {
            dialog.dismiss();
            showMessage(getString(R.string.main_coming_soon));
        });

        sheet.actionCloseDm.setOnClickListener(v -> {
            dialog.dismiss();
            showMessage(getString(R.string.main_coming_soon));
        });

        sheet.actionFavorite.setOnClickListener(v -> {
            dialog.dismiss();
            if (item == null || !item.hasChannelId()) {
                showMessage(getString(R.string.error_generic));
                return;
            }
            boolean nextFavorite = !item.isFavorite();
            viewModel.toggleConversationFavorite(item);
            showMessage(getString(nextFavorite ? R.string.dm_favorited_success : R.string.dm_unfavorited_success));
        });

        sheet.actionMarkRead.setOnClickListener(v -> {
            dialog.dismiss();
            showMessage(getString(R.string.main_coming_soon));
        });

        sheet.actionMuteConversation.setOnClickListener(v -> {
            dialog.dismiss();
            showMessage(getString(R.string.main_coming_soon));
        });

        dialog.show();
    }

    private void setupActions(View view) {
        binding.btnSearch.setOnClickListener(v ->
                Snackbar.make(view, getString(R.string.main_coming_soon), Snackbar.LENGTH_SHORT).show());

        binding.btnAddFriend.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), com.example.hubble.view.friend.AddFriendActivity.class);
            startActivity(intent);
        });

        binding.fabNewDm.setOnClickListener(v ->
                startActivity(NewMessageActivity.createIntent(requireContext())));
    }

    private void showMessage(String message) {
        if (binding == null || message == null || message.trim().isEmpty()) {
            return;
        }
        Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_SHORT).show();
    }

    private void openDmChat(@Nullable String channelId, @Nullable String displayName, @Nullable String avatarUrl) {
        if (channelId == null || channelId.trim().isEmpty()) {
            showMessage(getString(R.string.error_generic));
            return;
        }

        String safeDisplayName = displayName;
        if (safeDisplayName == null || safeDisplayName.trim().isEmpty()) {
            safeDisplayName = getString(R.string.dm_default_user);
        }

        startActivity(DmChatActivity.createIntent(requireContext(), channelId, safeDisplayName, avatarUrl));
    }

    private void bindConversationAvatar(@NonNull ImageView imageView, @Nullable String avatarUrl, @Nullable String displayName) {
        int avatarSize = imageView.getLayoutParams() != null ? imageView.getLayoutParams().width : imageView.getWidth();
        android.graphics.drawable.Drawable avatarFallback =
                AvatarPlaceholderUtils.createAvatarDrawable(
                        imageView.getContext(),
                        displayName,
                        avatarSize
                );

        Glide.with(imageView.getContext())
                .load(toAbsoluteAvatarUrl(avatarUrl))
                .placeholder(avatarFallback)
                .error(avatarFallback)
                .fallback(avatarFallback)
                .circleCrop()
                .into(imageView);
    }

    @Nullable
    private String toAbsoluteAvatarUrl(@Nullable String avatarUrl) {
        return NetworkConfig.resolveUrl(avatarUrl);
    }

    @Nullable
    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }

        for (String value : values) {
            if (!TextUtils.isEmpty(value) && !value.trim().isEmpty()) {
                return value;
            }
        }
        return null;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
