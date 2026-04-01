package com.example.hubble.view.me;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.hubble.R;
import com.example.hubble.databinding.FragmentMeBinding;
import com.example.hubble.utils.ImageSaveHelper;
import com.example.hubble.utils.TokenManager;
import com.example.hubble.view.auth.LoginActivity;
import com.example.hubble.view.friend.BlockedUsersActivity;
import com.example.hubble.view.friend.OutgoingRequestsActivity;
import com.example.hubble.view.settings.SettingsActivity;
import com.example.hubble.viewmodel.AuthViewModel;
import com.google.android.material.snackbar.Snackbar;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

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


        binding.btnSaveQr.setOnClickListener(v -> saveQrAsPng());

        binding.btnSettings.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), SettingsActivity.class)));

        binding.btnLogout.setOnClickListener(v -> logout());

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

        binding.cardNotes.setOnClickListener(v ->
                Snackbar.make(binding.getRoot(),
                        getString(R.string.main_coming_soon),
                        Snackbar.LENGTH_SHORT).show());

        if (savedInstanceState == null) {
            getChildFragmentManager()
                    .beginTransaction()
                    .replace(binding.profileFragmentContainer.getId(), new UserProfileFragment())
                    .commit();
        }
    }

    private void saveQrAsPng() {
        ProfileQrFragment qrFragment = getQrFragment();
        Bitmap bitmap = qrFragment != null ? qrFragment.getCurrentQrBitmap() : null;
        if (bitmap == null) {
            Snackbar.make(binding.getRoot(), R.string.me_qr_save_unavailable, Snackbar.LENGTH_LONG).show();
            return;
        }

        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        String fileName = "hubble_qr_" + timestamp + ".png";

        try {
            Uri savedUri = ImageSaveHelper.saveBitmapAsPng(requireContext(), bitmap, fileName);
            Snackbar.make(binding.getRoot(),
                    getString(R.string.me_qr_saved, savedUri.toString()),
                    Snackbar.LENGTH_LONG).show();
        } catch (Exception e) {
            Snackbar.make(binding.getRoot(),
                    getString(R.string.me_qr_save_failed),
                    Snackbar.LENGTH_LONG).show();
        }
    }

    @Nullable
    private ProfileQrFragment getQrFragment() {
        Fragment fragment = getChildFragmentManager().findFragmentById(R.id.meContentContainer);
        if (fragment instanceof ProfileQrFragment) {
            return (ProfileQrFragment) fragment;
        }
        return null;
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
