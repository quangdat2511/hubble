package com.example.hubble.view.me;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.hubble.R;
import com.example.hubble.databinding.FragmentMeBinding;
import com.example.hubble.view.friend.BlockedUsersActivity;
import com.example.hubble.view.friend.OutgoingRequestsActivity;
import com.example.hubble.view.settings.SettingsActivity;
import com.google.android.material.snackbar.Snackbar;
import androidx.fragment.app.Fragment;

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

        binding.btnOutgoingRequests.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), OutgoingRequestsActivity.class)));

        binding.btnBlockedUsers.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), BlockedUsersActivity.class)));

        binding.btnAddStatus.setOnClickListener(v ->
                Snackbar.make(binding.getRoot(),
                        getString(R.string.main_coming_soon),
                        Snackbar.LENGTH_SHORT).show());

        binding.cardFriends.setOnClickListener(v ->
                Snackbar.make(binding.getRoot(),
                        getString(R.string.main_coming_soon),
                        Snackbar.LENGTH_SHORT).show());

        binding.cardQr.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), QrHubActivity.class)));

        binding.cardNotes.setOnClickListener(v ->
                Snackbar.make(binding.getRoot(),
                        getString(R.string.main_coming_soon),
                        Snackbar.LENGTH_SHORT).show());

        if (getChildFragmentManager().findFragmentById(binding.profileFragmentContainer.getId()) == null) {
            getChildFragmentManager()
                    .beginTransaction()
                    .replace(binding.profileFragmentContainer.getId(), new UserProfileFragment())
                    .commit();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
