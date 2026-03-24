package com.example.hubble.view.me;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.hubble.databinding.FragmentMeBinding;
import com.example.hubble.utils.TokenManager;
import com.example.hubble.view.auth.LoginActivity;
import com.example.hubble.view.settings.SettingsActivity;

public class MeFragment extends Fragment {

    private FragmentMeBinding binding;

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

        binding.btnSettings.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), SettingsActivity.class)));

        binding.btnLogout.setOnClickListener(v -> logout());

        if (savedInstanceState == null) {
            getChildFragmentManager()
                    .beginTransaction()
                    .replace(binding.profileFragmentContainer.getId(), new UserProfileFragment())
                    .commit();
        }
    }

    private void logout() {
        TokenManager tokenManager = new TokenManager(requireContext());
        tokenManager.clear();

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