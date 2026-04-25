package com.example.hubble.view.search;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.example.hubble.R;
import com.example.hubble.data.api.NetworkConfig;
import com.example.hubble.data.model.auth.AuthResult;
import com.example.hubble.data.model.search.SearchMemberDto;
import com.example.hubble.data.repository.DmRepository;
import com.example.hubble.data.repository.FriendRepository;
import com.example.hubble.databinding.BottomSheetSearchUserPreviewBinding;
import com.example.hubble.utils.AvatarPlaceholderUtils;
import com.example.hubble.utils.TokenManager;
import com.example.hubble.view.dm.DmChatActivity;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class SearchUserPreviewBottomSheet extends BottomSheetDialogFragment {
    public static final String RESULT_KEY_RELATION_UPDATED = "search_relation_updated";
    public static final String RESULT_BUNDLE_KEY_UPDATED = "updated";

    private static final String ARG_ID = "id";
    private static final String ARG_USERNAME = "username";
    private static final String ARG_DISPLAY_NAME = "display_name";
    private static final String ARG_AVATAR_URL = "avatar_url";
    private static final String ARG_STATUS = "status";
    private static final String ARG_IS_SELF = "is_self";
    private static final String ARG_IS_FRIEND = "is_friend";
    private static final String ARG_FRIENDSHIP_STATE = "friendship_state";

    public static SearchUserPreviewBottomSheet newInstance(SearchMemberDto member) {
        SearchUserPreviewBottomSheet sheet = new SearchUserPreviewBottomSheet();
        Bundle args = new Bundle();
        args.putString(ARG_ID, member.getId());
        args.putString(ARG_USERNAME, member.getUsername());
        args.putString(ARG_DISPLAY_NAME, member.getDisplayName());
        args.putString(ARG_AVATAR_URL, member.getAvatarUrl());
        args.putString(ARG_STATUS, member.getStatus());
        args.putBoolean(ARG_IS_SELF, member.isSelf());
        args.putBoolean(ARG_IS_FRIEND, member.isFriend());
        args.putString(ARG_FRIENDSHIP_STATE, member.getFriendshipState());
        sheet.setArguments(args);
        return sheet;
    }

    private BottomSheetSearchUserPreviewBinding binding;
    private DmRepository dmRepository;
    private FriendRepository friendRepository;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = BottomSheetSearchUserPreviewBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Bundle args = getArguments();
        if (args == null) return;

        dmRepository = new DmRepository(requireContext());
        friendRepository = new FriendRepository(requireContext());

        String userId = args.getString(ARG_ID, "");
        String username = args.getString(ARG_USERNAME, "");
        String displayName = args.getString(ARG_DISPLAY_NAME, "");
        String avatarUrl = args.getString(ARG_AVATAR_URL, "");
        String status = args.getString(ARG_STATUS, "");
        boolean isSelf = args.getBoolean(ARG_IS_SELF, false);
        boolean isFriend = args.getBoolean(ARG_IS_FRIEND, false);
        String friendshipState = args.getString(ARG_FRIENDSHIP_STATE, "NONE");

        String resolvedName = (displayName != null && !displayName.isBlank()) ? displayName : username;
        String resolvedStatus = (status != null && !status.isBlank()) ? status : getString(R.string.status_offline);

        binding.tvDisplayName.setText(resolvedName);
        binding.tvUsername.setText(username == null || username.isBlank() ? "" : "@" + username);
        binding.tvStatusBadge.setText(resolvedStatus.toUpperCase());
        binding.tvCustomStatus.setText(getString(R.string.search_profile_status_hint, resolvedStatus));

        String resolvedAvatarUrl = NetworkConfig.resolveUrl(avatarUrl);
        Glide.with(binding.ivAvatar.getContext())
                .load(resolvedAvatarUrl)
                .placeholder(AvatarPlaceholderUtils.createAvatarDrawable(
                        binding.ivAvatar.getContext(), resolvedName, 56))
                .error(AvatarPlaceholderUtils.createAvatarDrawable(
                        binding.ivAvatar.getContext(), resolvedName, 56))
                .circleCrop()
                .into(binding.ivAvatar);

        applyFriendButtonState(isSelf, isFriend, friendshipState);

        binding.btnMessage.setOnClickListener(v -> startDm(userId, resolvedName, avatarUrl));
        binding.btnAddFriend.setOnClickListener(v -> sendFriendRequest(userId));
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

    private void startDm(String userId, String displayName, String avatarUrl) {
        if (userId == null || userId.isBlank()) return;
        dmRepository.getOrCreateDirectChannel(userId, result -> {
            if (result.getStatus() == AuthResult.Status.SUCCESS && result.getData() != null) {
                dmRepository.rememberOpenedDirectChannel(result.getData().getId());
                Intent intent = DmChatActivity.createIntent(
                        requireContext(),
                        result.getData().getId(),
                        displayName,
                        avatarUrl
                );
                startActivity(intent);
                dismissAllowingStateLoss();
                return;
            }
            String message = result.getMessage() != null ? result.getMessage() : getString(R.string.error_generic);
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
        });
    }

    private void sendFriendRequest(String userId) {
        if (userId == null || userId.isBlank()) return;
        TokenManager tokenManager = new TokenManager(requireContext());
        String myUserId = tokenManager.getUser() != null ? tokenManager.getUser().getId() : null;
        if (myUserId != null && myUserId.equals(userId)) return;
        friendRepository.sendRequestById(userId, result -> {
            String message;
            if (result.getStatus() == AuthResult.Status.SUCCESS) {
                message = getString(R.string.friend_request_send_success);
                applyFriendButtonState(false, false, "PENDING_OUTGOING");
                Bundle bundle = new Bundle();
                bundle.putBoolean(RESULT_BUNDLE_KEY_UPDATED, true);
                getParentFragmentManager().setFragmentResult(RESULT_KEY_RELATION_UPDATED, bundle);
            } else {
                message = result.getMessage() != null ? result.getMessage() : getString(R.string.friend_request_send_error);
            }
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void applyFriendButtonState(boolean isSelf, boolean isFriend, @Nullable String friendshipState) {
        if (isSelf) {
            binding.btnMessage.setVisibility(View.GONE);
            binding.btnAddFriend.setVisibility(View.GONE);
            return;
        }

        binding.btnMessage.setVisibility(View.VISIBLE);
        binding.btnAddFriend.setVisibility(View.VISIBLE);
        String state = friendshipState == null ? "NONE" : friendshipState;
        switch (state) {
            case "FRIEND":
                binding.btnAddFriend.setEnabled(false);
                binding.btnAddFriend.setText(R.string.search_friend_state_friend);
                binding.btnAddFriend.setAlpha(0.7f);
                return;
            case "PENDING_OUTGOING":
                binding.btnAddFriend.setEnabled(false);
                binding.btnAddFriend.setText(R.string.search_friend_state_pending_outgoing);
                binding.btnAddFriend.setAlpha(0.7f);
                return;
            case "PENDING_INCOMING":
                binding.btnAddFriend.setEnabled(false);
                binding.btnAddFriend.setText(R.string.search_friend_state_pending_incoming);
                binding.btnAddFriend.setAlpha(0.7f);
                return;
            default:
                if (isFriend) {
                    binding.btnAddFriend.setEnabled(false);
                    binding.btnAddFriend.setText(R.string.search_friend_state_friend);
                    binding.btnAddFriend.setAlpha(0.7f);
                } else {
                    binding.btnAddFriend.setEnabled(true);
                    binding.btnAddFriend.setText(R.string.new_message_add_friend);
                    binding.btnAddFriend.setAlpha(1.0f);
                }
        }
    }
}
