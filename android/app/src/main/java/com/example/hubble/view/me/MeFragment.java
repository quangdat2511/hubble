package com.example.hubble.view.me;

import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.hubble.R;
import com.example.hubble.data.model.auth.UserResponse;
import com.example.hubble.data.repository.AuthRepository;
import com.example.hubble.databinding.FragmentMeBinding;
import com.example.hubble.view.auth.LoginActivity;
import com.example.hubble.view.settings.SettingsActivity;
import com.example.hubble.viewmodel.AuthViewModel;
import com.example.hubble.viewmodel.AuthViewModelFactory;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.shape.ShapeAppearanceModel;
import com.google.android.material.snackbar.Snackbar;

public class MeFragment extends Fragment {

    private FragmentMeBinding binding;
    private AuthViewModel authViewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentMeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        authViewModel = new ViewModelProvider(requireActivity(),
                new AuthViewModelFactory(new AuthRepository(requireContext())))
                .get(AuthViewModel.class);

        populateUserInfo(authViewModel.getCurrentUser());
        setupActions(view);
    }

    private void populateUserInfo(@Nullable UserResponse user) {
        String displayName = user != null && user.getDisplayName() != null
                ? user.getDisplayName() : "User";
        String email = user != null && user.getEmail() != null
                ? user.getEmail() : "";

        binding.tvDisplayName.setText(displayName);
        binding.tvUsername.setText(email);

        String initials = displayName.isEmpty() ? "U"
                : String.valueOf(displayName.charAt(0)).toUpperCase();

        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.OVAL);
        bg.setColor(ContextCompat.getColor(requireContext(), R.color.color_primary));
        binding.ivAvatar.setImageDrawable(null);
        binding.ivAvatar.setBackground(bg);
        binding.ivAvatar.setShapeAppearanceModel(
                ShapeAppearanceModel.builder().setAllCornerSizes(999f).build());

        binding.tvAvatarInitials.setText(initials);
        binding.tvJoinedDate.setText(getString(R.string.app_name));
    }

    private void setupActions(View view) {
        binding.btnSettings.setOnClickListener(v -> openSettings());
        binding.btnOpenSettings.setOnClickListener(v -> openSettings());
        binding.btnLogout.setOnClickListener(v -> confirmLogout());

        binding.btnAddStatus.setOnClickListener(v ->
                Snackbar.make(view,
                        getString(R.string.main_coming_soon),
                        Snackbar.LENGTH_SHORT).show());
        binding.btnEditProfile.setOnClickListener(v ->
                Snackbar.make(view,
                        getString(R.string.main_coming_soon),
                        Snackbar.LENGTH_SHORT).show());
        binding.cardFriends.setOnClickListener(v ->
                Snackbar.make(view,
                        getString(R.string.main_coming_soon),
                        Snackbar.LENGTH_SHORT).show());
        binding.cardNotes.setOnClickListener(v ->
                Snackbar.make(view,
                        getString(R.string.main_coming_soon),
                        Snackbar.LENGTH_SHORT).show());
    }

    private void openSettings() {
        startActivity(new Intent(requireContext(), SettingsActivity.class));
    }

    private void confirmLogout() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.settings_logout_confirm_title))
                .setMessage(getString(R.string.settings_logout_confirm_message))
                .setNegativeButton(getString(R.string.settings_logout_confirm_no),
                        (dialog, which) -> dialog.dismiss())
                .setPositiveButton(getString(R.string.settings_logout_confirm_yes),
                        (dialog, which) -> {
                            authViewModel.logout();
                            navigateToLogin();
                        })
                .show();
    }

    private void navigateToLogin() {
        Intent intent = new Intent(requireContext(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().finish();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
