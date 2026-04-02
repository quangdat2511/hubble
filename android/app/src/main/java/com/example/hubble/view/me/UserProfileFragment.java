package com.example.hubble.view.me;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.hubble.R;
import com.example.hubble.data.model.auth.AuthResult;
import com.example.hubble.data.model.auth.UserResponse;
import com.example.hubble.data.model.me.UpdateProfileRequest;
import com.example.hubble.data.repository.UserRepository;
import com.example.hubble.databinding.FragmentUserProfileBinding;
import com.example.hubble.utils.InAppMessageUtils;

import java.util.Locale;

public class UserProfileFragment extends Fragment implements AvatarFragment.AvatarListener {

    private FragmentUserProfileBinding binding;
    private UserRepository userRepository;
    private UserResponse currentProfile;
    private boolean isEditMode = false;

    public UserProfileFragment() {
        super(R.layout.fragment_user_profile);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentUserProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        userRepository = new UserRepository(requireContext());
        setupAvatarFragment();
        setupStatusDropdown();
        setEditMode(false);
        setupActions();
        loadProfile();
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

    private void setupActions() {
        binding.btnSaveProfile.setOnClickListener(v -> {
            if (!isEditMode) {
                setEditMode(true);
            } else {
                saveProfile();
            }
        });
    }

    private void loadProfile() {
        userRepository.getProfile(result -> handleProfileResult(result, true));
    }

    private void setupAvatarFragment() {
        if (getChildFragmentManager().findFragmentById(R.id.avatarFragmentContainer) == null) {
            getChildFragmentManager()
                    .beginTransaction()
                    .replace(R.id.avatarFragmentContainer, new AvatarFragment())
                    .commit();
        }
    }

    private void populateUserInfo(@Nullable UserResponse user) {
        if (user == null) return;

        currentProfile = user;
        AvatarFragment avatarFragment = (AvatarFragment) getChildFragmentManager()
                .findFragmentById(R.id.avatarFragmentContainer);
        if (avatarFragment != null) {
            avatarFragment.renderUser(user);
        }
        binding.tvUsername.setText(safe(user.getUsername()));
        binding.etDisplayName.setText(safe(user.getDisplayName()));
        binding.etPhone.setText(safe(user.getPhone()));
        binding.etBio.setText(safe(user.getBio()));

        String status = safe(user.getStatus());
        if (status.isEmpty()) {
            binding.etStatus.setText("ONLINE", false);
        } else {
            binding.etStatus.setText(status, false);
        }
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private void setEditMode(boolean enabled) {
        isEditMode = enabled;

        setFieldEditable(binding.etDisplayName, enabled);
        setFieldEditable(binding.etPhone, enabled);
        setFieldEditable(binding.etBio, enabled);
        setStatusEditable(enabled);

        binding.btnSaveProfile.setText(
                enabled ? getString(R.string.me_save_profile)
                        : getString(R.string.me_edit_profile)
        );
    }

    private void setFieldEditable(EditText editText, boolean enabled) {
        editText.setEnabled(enabled);
        editText.setFocusable(enabled);
        editText.setFocusableInTouchMode(enabled);
        editText.setClickable(enabled);
        editText.setLongClickable(enabled);
        editText.setCursorVisible(enabled);
    }

    private void setStatusEditable(boolean enabled) {
        binding.etStatus.setEnabled(enabled);
        binding.etStatus.setClickable(enabled);
        binding.etStatus.setFocusable(enabled);
        binding.etStatus.setFocusableInTouchMode(enabled);
    }

    private void saveProfile() {
        String selectedStatus = resolveStatusForSave();

        UpdateProfileRequest request = new UpdateProfileRequest(
                binding.etBio.getText().toString().trim(),
                binding.etDisplayName.getText().toString().trim(),
                selectedStatus,
                binding.etPhone.getText().toString().trim()
        );

        userRepository.updateProfile(request, result -> handleProfileResult(result, false));
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

    private void handleProfileResult(AuthResult<UserResponse> result, boolean loadingProfile) {
        if (!isAdded() || binding == null || result == null) {
            return;
        }

        if (result.isSuccess() && result.getData() != null) {
            populateUserInfo(result.getData());
            if (!loadingProfile) {
                setEditMode(false);
                InAppMessageUtils.show(binding.getRoot(), getString(R.string.profile_updated));
            }
            return;
        }

        if (!result.isError()) {
            return;
        }

        String message = result.getMessage();
        InAppMessageUtils.showLong(binding.getRoot(), message);

        if (loadingProfile) {
            populateUserInfo(userRepository.getCachedUser());
        }
    }

    @Override
    public void onAvatarUpdated(@NonNull UserResponse updatedUser) {
        populateUserInfo(updatedUser);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
