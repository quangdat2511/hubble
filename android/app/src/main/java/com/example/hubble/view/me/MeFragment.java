package com.example.hubble.view.me;

import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.*;
import androidx.annotation.*;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.hubble.R;
import com.example.hubble.data.model.auth.UserResponse;
import com.example.hubble.data.repository.AuthRepository;
import com.example.hubble.databinding.FragmentMeBinding;
import com.example.hubble.view.settings.SettingsActivity;
import com.example.hubble.viewmodel.AuthViewModel;
import com.example.hubble.viewmodel.AuthViewModelFactory;
import com.google.android.material.snackbar.Snackbar;

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

        AuthViewModel vm = new ViewModelProvider(requireActivity(),
                new AuthViewModelFactory(new AuthRepository(requireContext())))
                .get(AuthViewModel.class);

        populateUserInfo(vm.getCurrentUser());
        setupActions(view);
    }

    private void populateUserInfo(@Nullable UserResponse user) {
        if (user == null) return;

        // Set text
        binding.tvUsername.setText(user.getUsername());
        binding.tvDisplayName.setText(user.getDisplayName());
        binding.tvPhone.setText(user.getPhone());
        binding.tvBio.setText(user.getBio());
        binding.tvStatus.setText(user.getStatus());

        // Avatar initials
        String name = user.getDisplayName() != null ? user.getDisplayName() : "U";
        String initials = name.substring(0, 1).toUpperCase();
        binding.tvAvatarInitials.setText(initials);

        // Avatar background
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.OVAL);
        bg.setColor(ContextCompat.getColor(requireContext(), R.color.color_primary));
        binding.ivAvatar.setBackground(bg);

        // Status dot color
        int color;
        switch (user.getStatus()) {
            case "ONLINE":
                color = android.R.color.holo_green_light;
                break;
            case "OFFLINE":
                color = android.R.color.darker_gray;
                break;
            default:
                color = android.R.color.holo_orange_light;
        }

        GradientDrawable dot = (GradientDrawable) binding.viewOnlineStatus.getBackground();
        dot.setColor(ContextCompat.getColor(requireContext(), color));
    }

    private void setupActions(View view) {
        binding.btnSettings.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), SettingsActivity.class)));

        binding.btnEditProfile.setOnClickListener(v ->
                Snackbar.make(view, "Edit coming soon", Snackbar.LENGTH_SHORT).show());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}