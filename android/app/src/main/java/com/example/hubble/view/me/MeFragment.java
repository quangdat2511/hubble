package com.example.hubble.view.me;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.hubble.R;
import com.example.hubble.databinding.FragmentMeBinding;
import com.example.hubble.view.friend.BlockedUsersActivity;
import com.example.hubble.view.friend.FriendsActivity;
import com.example.hubble.view.friend.OutgoingRequestsActivity;
import com.example.hubble.view.settings.SettingsActivity;

public class MeFragment extends Fragment {

    private FragmentMeBinding binding;
    private final ActivityResultLauncher<Intent> settingsLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == android.app.Activity.RESULT_OK && isAdded()) {
                    requireActivity().recreate();
                }
            });

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
                settingsLauncher.launch(new Intent(requireContext(), SettingsActivity.class)));

        binding.btnOutgoingRequests.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), OutgoingRequestsActivity.class)));

        binding.btnBlockedUsers.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), BlockedUsersActivity.class)));

        binding.cardFriends.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), FriendsActivity.class)));

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
