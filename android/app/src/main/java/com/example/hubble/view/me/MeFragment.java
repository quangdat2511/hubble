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
import com.google.android.material.snackbar.Snackbar;

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
        setupAvatarFragment();
        setupActions(view);
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

        binding.tvUsername.setText(user.getUsername());
    }

    private void setupActions(View view) {
        binding.btnSettings.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), SettingsActivity.class)));
        binding.btnLogout.setOnClickListener(v -> logout());
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

    private void logout() {
        new TokenManager(requireContext()).clear();

        Intent intent = new Intent(requireContext(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);

        requireActivity().finish();
    }
}