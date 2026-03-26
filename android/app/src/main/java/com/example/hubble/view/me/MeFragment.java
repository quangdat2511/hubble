package com.example.hubble.view.me;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.hubble.R;
import com.example.hubble.data.model.auth.UserResponse;
import com.example.hubble.data.repository.AuthRepository;
import com.example.hubble.databinding.FragmentMeBinding;
import com.example.hubble.utils.TokenManager;
import com.example.hubble.view.auth.LoginActivity;
import com.example.hubble.view.settings.SettingsActivity;
import com.example.hubble.viewmodel.AuthViewModel;
import com.example.hubble.viewmodel.AuthViewModelFactory;

public class MeFragment extends Fragment implements AvatarFragment.AvatarListener {

    private FragmentMeBinding binding;
    private AuthViewModel vm;

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

        vm = new ViewModelProvider(
                requireActivity(),
                new AuthViewModelFactory(new AuthRepository(requireContext()))
        ).get(AuthViewModel.class);

        populateUserInfo(vm.getCurrentUser());

        binding.btnSettings.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), SettingsActivity.class)));
        binding.btnLogout.setOnClickListener(v -> logout());
        binding.btnScanQr.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), ScanQrActivity.class)));
        binding.btnOpenQr.setOnClickListener(v -> openProfileQr());

        if (savedInstanceState == null) {
            getChildFragmentManager()
                    .beginTransaction()
                    .setReorderingAllowed(true)
                    .replace(binding.avatarFragmentContainer.getId(), new AvatarFragment())
                    .replace(binding.profileFragmentContainer.getId(), new UserProfileFragment())
                    .commit();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        populateUserInfo(new TokenManager(requireContext()).getUser());
    }

    private void populateUserInfo(@Nullable UserResponse user) {
        if (binding == null || user == null) {
            return;
        }

        String title = user.getDisplayName();
        if (title == null || title.trim().isEmpty()) {
            title = user.getUsername();
        }
        binding.tvUsername.setText(title == null ? "" : title);
    }

    @Override
    public void onAvatarUpdated(@NonNull UserResponse updatedUser) {
        populateUserInfo(updatedUser);
    }

    private void openProfileQr() {
        if (!isAdded()) {
            return;
        }

        requireActivity()
                .getSupportFragmentManager()
                .beginTransaction()
                .setReorderingAllowed(true)
                .replace(R.id.fragmentContainer, ProfileQrFragment.newInstance())
                .addToBackStack(ProfileQrFragment.class.getSimpleName())
                .commit();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void logout() {
        TokenManager tokenManager = new TokenManager(requireContext());
        tokenManager.clear();

        Intent intent = new Intent(requireContext(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);

        requireActivity().finish();
    }
}
