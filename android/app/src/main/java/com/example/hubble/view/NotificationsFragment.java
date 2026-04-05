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
import com.example.hubble.adapter.friend.FriendRequestAdapter;
import com.example.hubble.adapter.friend.NotificationActivityAdapter;
import com.example.hubble.adapter.notify.SystemNotificationAdapter;
import com.example.hubble.adapter.server.PendingInviteAdapter;
import com.example.hubble.data.model.dm.FriendRequestResponse;
import com.example.hubble.data.model.notify.NotificationResponse;
import com.example.hubble.data.model.server.ServerInviteResponse;
import com.example.hubble.data.repository.DmRepository;
import com.example.hubble.data.repository.FriendRepository;
import com.example.hubble.data.repository.NotificationRepository;
import com.example.hubble.data.repository.ServerInviteRepository;
import com.example.hubble.data.repository.ServerRepository;
import com.example.hubble.databinding.FragmentNotificationsBinding;
import com.example.hubble.viewmodel.FriendViewModel;
import com.example.hubble.viewmodel.FriendViewModelFactory;
import com.example.hubble.viewmodel.NotificationViewModel;
import com.example.hubble.viewmodel.NotificationViewModelFactory;
import com.example.hubble.viewmodel.home.MainViewModel;
import com.example.hubble.viewmodel.home.MainViewModelFactory;
import com.example.hubble.viewmodel.server.ServerInviteViewModel;
import com.example.hubble.viewmodel.server.ServerInviteViewModelFactory;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

public class NotificationsFragment extends Fragment {

    private FragmentNotificationsBinding binding;
    private ServerInviteViewModel inviteViewModel;
    private FriendViewModel friendViewModel;
    private NotificationViewModel notificationViewModel;
    private PendingInviteAdapter inviteAdapter;
    private FriendRequestAdapter friendRequestAdapter;
    private NotificationActivityAdapter activityAdapter;
    private SystemNotificationAdapter systemNotificationAdapter;

    private boolean invitesLoaded = false;
    private boolean friendRequestsLoaded = false;
    private boolean hasInvites = false;
    private boolean hasFriendRequests = false;
    private boolean hasRecentActivity = false;
    private boolean hasSystemNotifications = false;

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

        inviteViewModel = new ViewModelProvider(this,
                new ServerInviteViewModelFactory(new ServerInviteRepository(requireContext())))
                .get(ServerInviteViewModel.class);

        friendViewModel = new ViewModelProvider(this,
                new FriendViewModelFactory(new FriendRepository(requireContext())))
                .get(FriendViewModel.class);

        notificationViewModel = new ViewModelProvider(this,
                new NotificationViewModelFactory(new NotificationRepository(requireContext())))
                .get(NotificationViewModel.class);

        MainViewModel mainViewModel = new ViewModelProvider(requireActivity(),
                new MainViewModelFactory(
                        new DmRepository(requireContext()),
                        new ServerRepository(requireContext())))
                .get(MainViewModel.class);

        binding.btnNotificationsMore.setOnClickListener(v ->
                Snackbar.make(view, getString(R.string.main_coming_soon),
                        Snackbar.LENGTH_SHORT).show());

        setupRecyclerViews();
        observeFriendRequests();
        observeInvites(mainViewModel);
        observeRecentActivity();
        observeSystemNotifications();

        binding.progressBar.setVisibility(View.VISIBLE);
        friendViewModel.fetchIncomingRequests();
        inviteViewModel.loadMyInvites();
        friendViewModel.fetchOutgoingRequests();
        notificationViewModel.loadNotifications(0, 20);
        notificationViewModel.loadUnreadCount();
    }

    private void setupRecyclerViews() {
        friendRequestAdapter = new FriendRequestAdapter(new FriendRequestAdapter.OnRequestListener() {
            @Override
            public void onAccept(FriendRequestResponse request) {
                friendViewModel.acceptRequest(request.getId());
            }

            @Override
            public void onDecline(FriendRequestResponse request) {
                friendViewModel.declineRequest(request.getId());
            }
        });
        binding.rvFriendRequests.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvFriendRequests.setAdapter(friendRequestAdapter);

        inviteAdapter = new PendingInviteAdapter(new PendingInviteAdapter.OnInviteActionListener() {
            @Override
            public void onAccept(ServerInviteResponse invite) {
                inviteViewModel.acceptInvite(invite.getId());
            }

            @Override
            public void onDecline(ServerInviteResponse invite) {
                inviteViewModel.declineInvite(invite.getId());
            }
        });
        binding.rvPendingInvites.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvPendingInvites.setAdapter(inviteAdapter);

        activityAdapter = new NotificationActivityAdapter();
        binding.rvRecentActivity.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvRecentActivity.setAdapter(activityAdapter);

        systemNotificationAdapter = new SystemNotificationAdapter();
        systemNotificationAdapter.setListener(notification -> {
            if (!Boolean.TRUE.equals(notification.getIsRead())) {
                notificationViewModel.markAsRead(notification.getId());
            }
        });
        binding.rvSystemNotifications.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvSystemNotifications.setAdapter(systemNotificationAdapter);

        binding.btnMarkAllRead.setOnClickListener(v -> notificationViewModel.markAllAsRead());
    }

    private void observeFriendRequests() {
        friendViewModel.incomingRequests.observe(getViewLifecycleOwner(), result -> {
            if (result == null || result.isLoading()) return;

            friendRequestsLoaded = true;
            if (result.isSuccess()) {
                List<FriendRequestResponse> requests = result.getData();
                hasFriendRequests = requests != null && !requests.isEmpty();
                binding.rvFriendRequests.setVisibility(hasFriendRequests ? View.VISIBLE : View.GONE);
                if (hasFriendRequests) friendRequestAdapter.setRequests(requests);
            } else {
                hasFriendRequests = false;
                binding.rvFriendRequests.setVisibility(View.GONE);
            }
            updateLoadingAndEmptyState();
        });

        friendViewModel.actionState.observe(getViewLifecycleOwner(), result -> {
            if (result == null) return;
            if (result.isSuccess()) {
                Snackbar.make(binding.getRoot(),
                        getString(R.string.friend_request_respond_success), Snackbar.LENGTH_SHORT).show();
                friendViewModel.resetActionState();
                friendViewModel.fetchIncomingRequests();
            } else if (result.isError()) {
                Snackbar.make(binding.getRoot(),
                        result.getMessage() != null ? result.getMessage()
                                : getString(R.string.error_generic),
                        Snackbar.LENGTH_SHORT).show();
                friendViewModel.resetActionState();
            }
        });
    }

    private void observeInvites(MainViewModel mainViewModel) {
        inviteViewModel.getMyInvitesState().observe(getViewLifecycleOwner(), result -> {
            if (result == null || result.isLoading()) return;

            invitesLoaded = true;
            if (result.isSuccess()) {
                hasInvites = result.getData() != null && !result.getData().isEmpty();
                binding.rvPendingInvites.setVisibility(hasInvites ? View.VISIBLE : View.GONE);
                if (hasInvites) inviteAdapter.submitList(result.getData());
            } else {
                hasInvites = false;
                binding.rvPendingInvites.setVisibility(View.GONE);
            }
            updateLoadingAndEmptyState();
        });

        inviteViewModel.getRespondState().observe(getViewLifecycleOwner(), result -> {
            if (result == null) return;
            if (result.isSuccess()) {
                Snackbar.make(binding.getRoot(),
                        getString(R.string.invite_respond_success), Snackbar.LENGTH_SHORT).show();
                inviteViewModel.consumeRespondState();
            } else if (result.isError()) {
                Snackbar.make(binding.getRoot(),
                        result.getMessage() != null ? result.getMessage()
                                : getString(R.string.error_generic),
                        Snackbar.LENGTH_SHORT).show();
                inviteViewModel.consumeRespondState();
            }
        });

        inviteViewModel.getAcceptSuccessEvent().observe(getViewLifecycleOwner(), accepted -> {
            if (accepted == null || !accepted) return;
            mainViewModel.refreshServers();
            inviteViewModel.consumeAcceptSuccessEvent();
        });
    }

    private void observeRecentActivity() {
        friendViewModel.outgoingRequests.observe(getViewLifecycleOwner(), result -> {
            if (result == null || result.isLoading()) return;

            if (result.isSuccess() && result.getData() != null) {
                List<FriendRequestResponse> accepted = new ArrayList<>();
                for (FriendRequestResponse req : result.getData()) {
                    if ("ACCEPTED".equalsIgnoreCase(req.getStatus())) {
                        accepted.add(req);
                    }
                }
                hasRecentActivity = !accepted.isEmpty();
                boolean hasPending = hasFriendRequests || hasInvites;
                binding.dividerRecentActivity.setVisibility(
                        (hasPending && hasRecentActivity) ? View.VISIBLE : View.GONE);
                binding.tvRecentActivityLabel.setVisibility(hasRecentActivity ? View.VISIBLE : View.GONE);
                binding.rvRecentActivity.setVisibility(hasRecentActivity ? View.VISIBLE : View.GONE);
                if (hasRecentActivity) activityAdapter.setItems(accepted);
            } else {
                hasRecentActivity = false;
                binding.dividerRecentActivity.setVisibility(View.GONE);
                binding.tvRecentActivityLabel.setVisibility(View.GONE);
                binding.rvRecentActivity.setVisibility(View.GONE);
            }

            boolean hasPending = hasFriendRequests || hasInvites;
            binding.dividerRecentActivity.setVisibility(
                    (hasPending && hasRecentActivity) ? View.VISIBLE : View.GONE);
            updateScrollContentVisibility();
        });
    }

    private void observeSystemNotifications() {
        notificationViewModel.notifications.observe(getViewLifecycleOwner(), result -> {
            if (result == null || result.isLoading()) return;

            if (result.isSuccess()) {
                List<NotificationResponse> notifications = result.getData();
                hasSystemNotifications = notifications != null && !notifications.isEmpty();
                boolean hasOther = hasFriendRequests || hasInvites || hasRecentActivity;
                binding.dividerSystemNotifications.setVisibility(
                        (hasOther && hasSystemNotifications) ? View.VISIBLE : View.GONE);
                binding.layoutSystemNotificationsHeader.setVisibility(
                        hasSystemNotifications ? View.VISIBLE : View.GONE);
                binding.rvSystemNotifications.setVisibility(
                        hasSystemNotifications ? View.VISIBLE : View.GONE);
                if (hasSystemNotifications) systemNotificationAdapter.setItems(notifications);
            } else {
                hasSystemNotifications = false;
                binding.dividerSystemNotifications.setVisibility(View.GONE);
                binding.layoutSystemNotificationsHeader.setVisibility(View.GONE);
                binding.rvSystemNotifications.setVisibility(View.GONE);
            }
            updateScrollContentVisibility();
        });

        notificationViewModel.markReadState.observe(getViewLifecycleOwner(), result -> {
            if (result == null) return;
            if (result.isSuccess()) {
                notificationViewModel.resetMarkReadState();
            }
        });
    }

    private void updateLoadingAndEmptyState() {
        if (!friendRequestsLoaded || !invitesLoaded) return;

        binding.progressBar.setVisibility(View.GONE);
        updateScrollContentVisibility();
    }

    private void updateScrollContentVisibility() {
        boolean hasAny = hasFriendRequests || hasInvites || hasRecentActivity || hasSystemNotifications;
        binding.layoutEmpty.setVisibility(hasAny ? View.GONE : View.VISIBLE);
        binding.scrollContent.setVisibility(hasAny ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
