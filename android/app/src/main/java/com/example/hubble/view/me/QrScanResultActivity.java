package com.example.hubble.view.me;

import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.hubble.R;
import com.example.hubble.data.model.auth.UserResponse;
import com.example.hubble.data.repository.DmRepository;
import com.example.hubble.databinding.ActivityQrScanResultBinding;
import com.example.hubble.utils.TokenManager;
import com.example.hubble.view.auth.LoginActivity;
import com.example.hubble.view.base.BaseAuthActivity;
import com.google.android.material.shape.ShapeAppearanceModel;

public class QrScanResultActivity extends BaseAuthActivity {

    public static final String EXTRA_QR_TOKEN = "extra_qr_token";

    private ActivityQrScanResultBinding binding;
    private DmRepository dmRepository;
    private UserResponse scannedUser;

    public static String createQrDeepLink(String qrToken) {
        return "hubble://friend-qr?token=" + Uri.encode(qrToken);
    }

    @Override
    protected View getRootView() {
        return binding.getRoot();
    }

    @Override
    protected View getProgressBar() {
        return binding.progressOverlay;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityQrScanResultBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (new TokenManager(this).getUser() == null) {
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

        dmRepository = new DmRepository(this);
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        String qrToken = extractQrToken(getIntent());
        if (qrToken == null || qrToken.trim().isEmpty()) {
            showError(getString(R.string.me_qr_empty));
            renderEmptyState(getString(R.string.me_qr_invalid));
            return;
        }

        loadProfile(qrToken);
    }

    private void loadProfile(String qrToken) {
        setLoadingState(true);
        binding.tvHint.setText(R.string.me_qr_loading);
        binding.btnAddFriend.setEnabled(false);

        dmRepository.scanQrProfile(qrToken, result -> runOnUiThread(() -> {
            setLoadingState(false);
            if (!result.isSuccess() || result.getData() == null) {
                renderEmptyState(result.getMessage() != null ? result.getMessage() : getString(R.string.me_qr_invalid));
                return;
            }

            scannedUser = result.getData();
            bindProfile(scannedUser);
        }));
    }

    private void bindProfile(UserResponse user) {
        String displayName = user.getDisplayName() != null && !user.getDisplayName().trim().isEmpty()
                ? user.getDisplayName()
                : (user.getUsername() != null ? user.getUsername() : "User");
        String username = user.getUsername() != null && !user.getUsername().trim().isEmpty()
                ? "@" + user.getUsername()
                : "";
        String initials = displayName.isEmpty() ? "U" : String.valueOf(displayName.charAt(0)).toUpperCase();

        binding.tvDisplayName.setText(displayName);
        binding.tvUsername.setText(username);
        binding.tvHint.setText(R.string.me_qr_scan_title);

        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.OVAL);
        bg.setColor(ContextCompat.getColor(this, R.color.color_primary));
        binding.ivAvatar.setBackground(bg);
        binding.ivAvatar.setShapeAppearanceModel(
                ShapeAppearanceModel.builder().setAllCornerSizes(999f).build());
        binding.tvAvatarInitials.setText(initials);

        String currentUserId = dmRepository.getCurrentUserId();
        boolean isMe = currentUserId != null && currentUserId.equals(user.getId());
        binding.btnAddFriend.setEnabled(!isMe);
        binding.btnAddFriend.setText(isMe ? R.string.me_qr_you : R.string.me_qr_add_friend);
    }

    private void renderEmptyState(String message) {
        binding.tvDisplayName.setText(R.string.me_qr_scan_title);
        binding.tvUsername.setText("");
        binding.tvAvatarInitials.setText("?");
        binding.tvHint.setText(message);
        binding.btnAddFriend.setEnabled(false);
        binding.btnAddFriend.setText(R.string.me_qr_add_friend);
    }

    private String extractQrToken(Intent intent) {
        if (intent == null) {
            return null;
        }

        String extraToken = intent.getStringExtra(EXTRA_QR_TOKEN);
        if (extraToken != null && !extraToken.trim().isEmpty()) {
            return extraToken;
        }

        Uri data = intent.getData();
        if (data == null) {
            return null;
        }
        return data.getQueryParameter("token");
    }
}
