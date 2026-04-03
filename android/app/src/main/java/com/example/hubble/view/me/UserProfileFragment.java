package com.example.hubble.view.me;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.hubble.R;
import com.example.hubble.data.model.auth.AuthResult;
import com.example.hubble.data.model.auth.UserResponse;
import com.example.hubble.data.repository.UserRepository;
import com.example.hubble.databinding.FragmentUserProfileBinding;
import com.example.hubble.utils.AppLanguageManager;
import com.example.hubble.utils.InAppMessageUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class UserProfileFragment extends Fragment
        implements AvatarFragment.AvatarListener, ProfileEditBottomSheet.ProfileEditListener {

    private static final String VALUE_PLACEHOLDER = "-";
    private static final String EDIT_PROFILE_TAG = "edit_profile_sheet";

    private FragmentUserProfileBinding binding;
    private UserRepository userRepository;
    private UserResponse currentProfile;

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
        setupActions();
        loadProfile();
    }

    private void setupAvatarFragment() {
        if (getChildFragmentManager().findFragmentById(R.id.avatarFragmentContainer) == null) {
            getChildFragmentManager()
                    .beginTransaction()
                    .replace(R.id.avatarFragmentContainer, new AvatarFragment())
                    .commitNow();
        }

        AvatarFragment avatarFragment = getAvatarFragment();
        if (avatarFragment != null) {
            avatarFragment.setEditingEnabled(false);
        }
    }

    private void setupActions() {
        binding.btnSaveProfile.setOnClickListener(v -> openEditProfileSheet());
    }

    private void loadProfile() {
        userRepository.getProfile(result -> handleProfileResult(result, true));
    }

    private void handleProfileResult(AuthResult<UserResponse> result, boolean loadingProfile) {
        if (!isAdded() || binding == null || result == null) {
            return;
        }

        if (result.isSuccess() && result.getData() != null) {
            populateUserInfo(result.getData());
            return;
        }

        if (!result.isError()) {
            return;
        }

        InAppMessageUtils.showLong(binding.getRoot(), result.getMessage());
        if (loadingProfile) {
            populateUserInfo(userRepository.getCachedUser());
        }
    }

    private void populateUserInfo(@Nullable UserResponse user) {
        if (user == null || binding == null) {
            return;
        }

        currentProfile = user;
        AvatarFragment avatarFragment = getAvatarFragment();
        if (avatarFragment != null) {
            avatarFragment.renderUser(user);
        }

        String username = safe(user.getUsername());
        String displayName = resolveDisplayName(user);
        String status = resolveStatusLabel(safe(user.getStatus()));

        binding.tvDisplayName.setText(displayName);
        binding.tvUsername.setText(username.isEmpty() ? VALUE_PLACEHOLDER : "@" + username);
        binding.tvStatusBadge.setText(status);
        binding.tvJoinedSinceValue.setText(formatJoinedSince(user.getCreatedAt()));

    }

    private void openEditProfileSheet() {
        if (!isAdded()) {
            return;
        }

        Fragment existing = getChildFragmentManager().findFragmentByTag(EDIT_PROFILE_TAG);
        if (existing != null) {
            return;
        }

        ProfileEditBottomSheet.newInstance(currentProfile)
                .show(getChildFragmentManager(), EDIT_PROFILE_TAG);
    }

    private AvatarFragment getAvatarFragment() {
        Fragment child = getChildFragmentManager().findFragmentById(R.id.avatarFragmentContainer);
        return child instanceof AvatarFragment ? (AvatarFragment) child : null;
    }

    private String resolveDisplayName(@NonNull UserResponse user) {
        String displayName = safe(user.getDisplayName());
        if (displayName.isEmpty()) {
            displayName = safe(user.getUsername());
        }
        return displayName.isEmpty() ? VALUE_PLACEHOLDER : displayName;
    }

    private String resolveStatusLabel(String status) {
        return TextUtils.isEmpty(status) ? "ONLINE" : status.trim().toUpperCase(Locale.ROOT);
    }

    private String formatJoinedSince(@Nullable String createdAt) {
        if (TextUtils.isEmpty(createdAt)) {
            return VALUE_PLACEHOLDER;
        }

        String[] patterns = new String[] {
                "yyyy-MM-dd'T'HH:mm:ss.SSSX",
                "yyyy-MM-dd'T'HH:mm:ssX",
                "yyyy-MM-dd'T'HH:mm:ss",
                "yyyy-MM-dd"
        };

        for (String pattern : patterns) {
            try {
                SimpleDateFormat parser = new SimpleDateFormat(pattern, Locale.US);
                Date parsedDate = parser.parse(createdAt);
                if (parsedDate != null) {
                    return formatJoinedDateForCurrentLanguage(parsedDate);
                }
            } catch (ParseException ignored) {
                // Try the next known backend date shape.
            }
        }

        return createdAt;
    }

    private String formatJoinedDateForCurrentLanguage(@NonNull Date joinedDate) {
        boolean isVietnamese = AppLanguageManager.LANGUAGE_VIETNAMESE.equals(
                AppLanguageManager.getCurrentLanguage(requireContext())
        );

        SimpleDateFormat formatter = isVietnamese
                ? new SimpleDateFormat("d 'thg' M, yyyy", new Locale("vi"))
                : new SimpleDateFormat("MMM d, yyyy", Locale.ENGLISH);
        return formatter.format(joinedDate);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    @Override
    public void onAvatarUpdated(@NonNull UserResponse updatedUser) {
        populateUserInfo(updatedUser);
    }

    @Override
    public void onProfileEdited(@NonNull UserResponse updatedUser) {
        populateUserInfo(updatedUser);
        if (binding != null) {
            InAppMessageUtils.show(binding.getRoot(), getString(R.string.profile_updated));
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
