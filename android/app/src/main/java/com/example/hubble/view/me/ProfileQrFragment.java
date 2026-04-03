package com.example.hubble.view.me;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.hubble.R;
import com.example.hubble.data.model.auth.UserResponse;
import com.example.hubble.data.repository.AuthRepository;
import com.example.hubble.data.repository.DmRepository;
import com.example.hubble.utils.QrBitmapHelper;
import com.example.hubble.viewmodel.AuthViewModel;
import com.example.hubble.viewmodel.AuthViewModelFactory;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.snackbar.Snackbar;

public class ProfileQrFragment extends Fragment {

    private static final String ARG_EMBEDDED = "arg_embedded";

    private View rootView;
    private MaterialToolbar toolbar;
    private TextView tvDisplayName;
    private TextView tvUsername;
    private ImageView ivQrCode;
    private ProgressBar progressQr;
    private TextView tvHint;
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
        rootView = inflater.inflate(R.layout.fragment_profile_qr, container, false);
        bindViews(rootView);
        return rootView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        dmRepository = new DmRepository(requireContext());

        AuthViewModel authViewModel = new ViewModelProvider(requireActivity(),
                new AuthViewModelFactory(new AuthRepository(requireContext())))
                .get(AuthViewModel.class);

        boolean embedded = isEmbeddedMode();
        setupAvatarFragment();
        bindCurrentUser(authViewModel.getCurrentUser());
        configureToolbar(embedded);
        loadQrCode();
    }

    private void configureToolbar(boolean embedded) {
        if (embedded) {
            toolbar.setNavigationIcon(null);
            toolbar.setTitle(R.string.me_qr_title);
            return;
        }

        toolbar.setNavigationOnClickListener(v -> requireActivity().finish());
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

    private void bindCurrentUser(@Nullable UserResponse user) {
        String displayName = user != null && user.getDisplayName() != null
                ? user.getDisplayName()
                : "User";
        String username = user != null && user.getUsername() != null && !user.getUsername().trim().isEmpty()
                ? "@" + user.getUsername()
                : "";

        tvDisplayName.setText(displayName);
        tvUsername.setText(username);
        tvHint.setText(R.string.me_qr_opening_hint);

        AvatarFragment avatarFragment = getAvatarFragment();
        if (avatarFragment != null && user != null) {
            avatarFragment.renderUser(user);
        }
    }

    private void loadQrCode() {
        progressQr.setVisibility(View.VISIBLE);
        ivQrCode.setImageDrawable(null);
        tvHint.setText(R.string.me_qr_loading);
        currentQrBitmap = null;

        dmRepository.getMyQrToken(result -> {
            if (!isAdded() || rootView == null) {
                return;
            }

            progressQr.setVisibility(View.GONE);
            if (!result.isSuccess() || result.getData() == null) {
                Snackbar.make(rootView,
                        result.getMessage() != null ? result.getMessage() : getString(R.string.me_qr_unavailable),
                        Snackbar.LENGTH_LONG).show();
                tvHint.setText(R.string.me_qr_unavailable);
                return;
            }

            Bitmap bitmap = QrBitmapHelper.createQrBitmap(
                    QrScanResultActivity.createQrDeepLink(result.getData()),
                    640,
                    640
            );
            if (bitmap == null) {
                tvHint.setText(R.string.me_qr_unavailable);
                return;
            }

            currentQrBitmap = bitmap;
            ivQrCode.setImageBitmap(bitmap);
            tvHint.setText(R.string.me_qr_opening_hint);
        });
    }

    private void bindViews(@NonNull View view) {
        toolbar = view.findViewById(R.id.toolbar);
        tvDisplayName = view.findViewById(R.id.tvDisplayName);
        tvUsername = view.findViewById(R.id.tvUsername);
        ivQrCode = view.findViewById(R.id.ivQrCode);
        progressQr = view.findViewById(R.id.progressQr);
        tvHint = view.findViewById(R.id.tvHint);
    }

    @Nullable
    public Bitmap getCurrentQrBitmap() {
        return currentQrBitmap;
    }

    private boolean isEmbeddedMode() {
        Bundle args = getArguments();
        return args != null && args.getBoolean(ARG_EMBEDDED, false);
    }

    @Nullable
    private AvatarFragment getAvatarFragment() {
        Fragment child = getChildFragmentManager().findFragmentById(R.id.avatarFragmentContainer);
        return child instanceof AvatarFragment ? (AvatarFragment) child : null;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        rootView = null;
        toolbar = null;
        tvDisplayName = null;
        tvUsername = null;
        ivQrCode = null;
        progressQr = null;
        tvHint = null;
    }
}
