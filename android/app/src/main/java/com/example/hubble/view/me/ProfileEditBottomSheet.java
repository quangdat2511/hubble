package com.example.hubble.view.me;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.hubble.R;
import com.example.hubble.data.model.auth.AuthResult;
import com.example.hubble.data.model.auth.UserResponse;
import com.example.hubble.data.model.me.UpdateProfileRequest;
import com.example.hubble.data.repository.UserRepository;
import com.example.hubble.databinding.BottomSheetProfileEditBinding;
import com.example.hubble.utils.InAppMessageUtils;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.Locale;

public class ProfileEditBottomSheet extends BottomSheetDialogFragment implements AvatarFragment.AvatarListener {

    public interface ProfileEditListener {
        void onProfileEdited(@NonNull UserResponse updatedUser);
    }

    private static final String ARG_USERNAME = "arg_username";
    private static final String ARG_DISPLAY_NAME = "arg_display_name";
    private static final String ARG_PHONE = "arg_phone";
    private static final String ARG_BIO = "arg_bio";
    private static final String ARG_STATUS = "arg_status";
    private static final String ARG_AVATAR_URL = "arg_avatar_url";
    private static final String ARG_CREATED_AT = "arg_created_at";
    private static final String ARG_ID = "arg_id";

    private BottomSheetProfileEditBinding binding;
    private UserRepository userRepository;
    private UserResponse currentProfile;
    private boolean isSaving;

    public static ProfileEditBottomSheet newInstance(@Nullable UserResponse user) {
        ProfileEditBottomSheet sheet = new ProfileEditBottomSheet();
        Bundle args = new Bundle();
        if (user != null) {
            args.putString(ARG_ID, user.getId());
            args.putString(ARG_USERNAME, user.getUsername());
            args.putString(ARG_DISPLAY_NAME, user.getDisplayName());
            args.putString(ARG_PHONE, user.getPhone());
            args.putString(ARG_BIO, user.getBio());
            args.putString(ARG_STATUS, user.getStatus());
            args.putString(ARG_AVATAR_URL, user.getAvatarUrl());
            args.putString(ARG_CREATED_AT, user.getCreatedAt());
        }
        sheet.setArguments(args);
        return sheet;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = BottomSheetProfileEditBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        userRepository = new UserRepository(requireContext());

        setupStatusDropdown();
        setupAvatarFragment();
        setupActions();
        currentProfile = buildInitialProfile();
        populateUserInfo(currentProfile);
    }

    private void setupStatusDropdown() {
        String[] statuses = getResources().getStringArray(R.array.status_options);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_list_item_1,
                statuses
        );
        binding.etStatus.setAdapter(adapter);
    }

    private void setupAvatarFragment() {
        if (getChildFragmentManager().findFragmentById(R.id.avatarFragmentContainer) == null) {
            getChildFragmentManager()
                    .beginTransaction()
                    .replace(R.id.avatarFragmentContainer, new AvatarFragment())
                    .commitNow();
        }

        Fragment child = getChildFragmentManager().findFragmentById(R.id.avatarFragmentContainer);
        if (child instanceof AvatarFragment) {
            ((AvatarFragment) child).setEditingEnabled(true);
        }
    }

    private void setupActions() {
        binding.btnCloseEditor.setOnClickListener(v -> dismiss());
        binding.actionSave.setOnClickListener(v -> {
            if (!isSaving) {
                saveProfile();
            }
        });
    }

    private void populateUserInfo(@Nullable UserResponse user) {
        if (user == null || binding == null) {
            return;
        }

        currentProfile = user;
        binding.tvDisplayName.setText(resolveDisplayName(user));
        binding.tvUsername.setText(formatUsername(user.getUsername()));
        binding.etDisplayName.setText(safe(user.getDisplayName()));
        binding.etPhone.setText(safe(user.getPhone()));
        binding.etBio.setText(safe(user.getBio()));
        binding.etStatus.setText(resolveStatusLabel(user.getStatus()), false);

        Fragment child = getChildFragmentManager().findFragmentById(R.id.avatarFragmentContainer);
        if (child instanceof AvatarFragment) {
            ((AvatarFragment) child).renderUser(user);
        }
    }

    private void saveProfile() {
        UpdateProfileRequest request = new UpdateProfileRequest(
                textOf(binding.etBio),
                textOf(binding.etDisplayName),
                resolveStatusForSave(),
                textOf(binding.etPhone)
        );

        setSaving(true);
        userRepository.updateProfile(request, this::handleSaveResult);
    }

    private void handleSaveResult(AuthResult<UserResponse> result) {
        if (!isAdded() || binding == null || result == null) {
            return;
        }

        if (result.isLoading()) {
            setSaving(true);
            return;
        }

        setSaving(false);
        if (result.isSuccess() && result.getData() != null) {
            notifyHost(result.getData());
            dismissAllowingStateLoss();
            return;
        }

        if (result.isError()) {
            InAppMessageUtils.showLong(binding.getRoot(), result.getMessage());
        }
    }

    private void setSaving(boolean saving) {
        isSaving = saving;
        binding.actionSave.setEnabled(!saving);
        binding.actionSave.setAlpha(saving ? 0.5f : 1f);
    }

    private String resolveStatusForSave() {
        String selectedStatus = binding.etStatus.getText() != null
                ? binding.etStatus.getText().toString().trim()
                : "";

        if (selectedStatus.isEmpty() && currentProfile != null) {
            selectedStatus = safe(currentProfile.getStatus()).trim();
        }
        if (selectedStatus.isEmpty()) {
            selectedStatus = "ONLINE";
        }
        return selectedStatus.toUpperCase(Locale.ROOT);
    }

    private void notifyHost(@NonNull UserResponse updatedUser) {
        currentProfile = updatedUser;
        if (getParentFragment() instanceof ProfileEditListener) {
            ((ProfileEditListener) getParentFragment()).onProfileEdited(updatedUser);
        } else if (getActivity() instanceof ProfileEditListener) {
            ((ProfileEditListener) getActivity()).onProfileEdited(updatedUser);
        }
    }

    private UserResponse buildInitialProfile() {
        Bundle args = getArguments();
        if (args == null || args.isEmpty()) {
            UserResponse cachedUser = userRepository.getCachedUser();
            return cachedUser != null ? cachedUser : new UserResponse();
        }

        UserResponse user = new UserResponse();
        user.setId(args.getString(ARG_ID));
        user.setUsername(args.getString(ARG_USERNAME));
        user.setDisplayName(args.getString(ARG_DISPLAY_NAME));
        user.setPhone(args.getString(ARG_PHONE));
        user.setBio(args.getString(ARG_BIO));
        user.setStatus(args.getString(ARG_STATUS));
        user.setAvatarUrl(args.getString(ARG_AVATAR_URL));
        user.setCreatedAt(args.getString(ARG_CREATED_AT));
        return user;
    }

    private String resolveDisplayName(@NonNull UserResponse user) {
        String displayName = safe(user.getDisplayName());
        if (displayName.isEmpty()) {
            displayName = safe(user.getUsername());
        }
        return displayName.isEmpty() ? "-" : displayName;
    }

    private String formatUsername(@Nullable String username) {
        String safeUsername = safe(username);
        return safeUsername.isEmpty() ? "-" : "@" + safeUsername;
    }

    private String resolveStatusLabel(@Nullable String status) {
        return TextUtils.isEmpty(status) ? "ONLINE" : status.trim().toUpperCase(Locale.ROOT);
    }

    private String textOf(@NonNull android.widget.TextView textView) {
        return textView.getText() != null ? textView.getText().toString().trim() : "";
    }

    private String safe(@Nullable String value) {
        return value == null ? "" : value;
    }

    @Override
    public void onAvatarUpdated(@NonNull UserResponse updatedUser) {
        populateUserInfo(updatedUser);
        notifyHost(updatedUser);
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
