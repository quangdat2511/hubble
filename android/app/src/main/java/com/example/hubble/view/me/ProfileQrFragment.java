package com.example.hubble.view.me;

import android.graphics.Bitmap;
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
import com.example.hubble.data.repository.DmRepository;
import com.example.hubble.databinding.FragmentProfileQrBinding;
import com.example.hubble.utils.QrBitmapHelper;
import com.example.hubble.viewmodel.AuthViewModel;
import com.example.hubble.viewmodel.AuthViewModelFactory;
import com.google.android.material.shape.ShapeAppearanceModel;
import com.google.android.material.snackbar.Snackbar;

public class ProfileQrFragment extends Fragment {

    private static final String ARG_EMBEDDED = "arg_embedded";

    private FragmentProfileQrBinding binding;
    private DmRepository dmRepository;
    private Bitmap currentQrBitmap;

    public static ProfileQrFragment newInstance() {
        return createInstance(false);
    }

    public static ProfileQrFragment newEmbeddedInstance() {
        return createInstance(true);
    }

    private static ProfileQrFragment createInstance(boolean embedded) {
        ProfileQrFragment fragment = new ProfileQrFragment();
        Bundle args = new Bundle();
        args.putBoolean(ARG_EMBEDDED, embedded);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentProfileQrBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        dmRepository = new DmRepository(requireContext());

        AuthViewModel authViewModel = new ViewModelProvider(requireActivity(),
                new AuthViewModelFactory(new AuthRepository(requireContext())))
                .get(AuthViewModel.class);

        boolean embedded = isEmbeddedMode();
        bindCurrentUser(authViewModel.getCurrentUser());
        configureToolbar(embedded);
        loadQrCode();
    }

    private void configureToolbar(boolean embedded) {
        if (embedded) {
            binding.toolbar.setNavigationIcon(null);
            binding.toolbar.setTitle(R.string.me_qr_title);
            return;
        }

        binding.toolbar.setNavigationOnClickListener(v -> requireActivity()
                .getSupportFragmentManager()
                .popBackStack());
    }

    private void bindCurrentUser(@Nullable UserResponse user) {
        String displayName = user != null && user.getDisplayName() != null
                ? user.getDisplayName()
                : "User";
        String username = user != null && user.getUsername() != null && !user.getUsername().trim().isEmpty()
                ? "@" + user.getUsername()
                : "";
        String initials = displayName.isEmpty() ? "U"
                : String.valueOf(displayName.charAt(0)).toUpperCase();

        binding.tvDisplayName.setText(displayName);
        binding.tvUsername.setText(username);
        binding.tvHint.setText(R.string.me_qr_opening_hint);

        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.OVAL);
        bg.setColor(ContextCompat.getColor(requireContext(), R.color.color_primary));
        binding.ivAvatar.setBackground(bg);
        binding.ivAvatar.setShapeAppearanceModel(
                ShapeAppearanceModel.builder().setAllCornerSizes(999f).build());
        binding.tvAvatarInitials.setText(initials);
    }

    private void loadQrCode() {
        binding.progressQr.setVisibility(View.VISIBLE);
        binding.ivQrCode.setImageDrawable(null);
        binding.tvHint.setText(R.string.me_qr_loading);
        currentQrBitmap = null;

        dmRepository.getMyQrToken(result -> {
            if (!isAdded() || binding == null) {
                return;
            }

            binding.progressQr.setVisibility(View.GONE);
            if (!result.isSuccess() || result.getData() == null) {
                Snackbar.make(binding.getRoot(),
                        result.getMessage() != null ? result.getMessage() : getString(R.string.me_qr_unavailable),
                        Snackbar.LENGTH_LONG).show();
                binding.tvHint.setText(R.string.me_qr_unavailable);
                return;
            }

            Bitmap bitmap = QrBitmapHelper.createQrBitmap(
                    QrScanResultActivity.createQrDeepLink(result.getData()),
                    640,
                    640
            );
            if (bitmap == null) {
                binding.tvHint.setText(R.string.me_qr_unavailable);
                return;
            }

            currentQrBitmap = bitmap;
            binding.ivQrCode.setImageBitmap(bitmap);
            binding.tvHint.setText(R.string.me_qr_opening_hint);
        });
    }

    @Nullable
    public Bitmap getCurrentQrBitmap() {
        return currentQrBitmap;
    }

    private boolean isEmbeddedMode() {
        Bundle args = getArguments();
        return args != null && args.getBoolean(ARG_EMBEDDED, false);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
