package com.example.hubble.view.me;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.example.hubble.R;
import com.example.hubble.data.api.NetworkConfig;
import com.example.hubble.data.model.auth.AuthResult;
import com.example.hubble.data.model.auth.UserResponse;
import com.example.hubble.data.model.dm.FriendRequestResponse;
import com.example.hubble.data.repository.DmRepository;
import com.example.hubble.data.repository.FriendRepository;
import com.example.hubble.databinding.ActivityQrScanResultBinding;
import com.example.hubble.utils.AvatarPlaceholderUtils;
import com.example.hubble.utils.TokenManager;
import com.example.hubble.view.auth.LoginActivity;
import com.example.hubble.view.base.BaseAuthActivity;
import com.google.android.material.snackbar.Snackbar;

import retrofit2.HttpException;

public class QrScanResultActivity extends BaseAuthActivity {

    public static final String EXTRA_QR_TOKEN = "extra_qr_token";

    private ActivityQrScanResultBinding binding;
    private DmRepository dmRepository;
    private FriendRepository friendRepository;
    private UserResponse scannedUser;
    private String pendingQrToken;
    private boolean friendRequestSent;
    private boolean friendRequestInFlight;

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
        applyEdgeToEdge(binding.getRoot());

        if (new TokenManager(this).getUser() == null) {
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

        dmRepository = new DmRepository(this);
        friendRepository = new FriendRepository(this);

        binding.toolbar.setNavigationOnClickListener(v -> navigateBack());
        binding.btnRetry.setOnClickListener(v -> retryLoadingProfile());
        binding.btnAddFriend.setOnClickListener(v -> sendFriendRequest());
        binding.ivStateIcon.setImageDrawable(
                AvatarPlaceholderUtils.createDefaultAvatarDrawable(this, dpToPx(56))
        );

        pendingQrToken = extractQrToken(getIntent());
        if (TextUtils.isEmpty(pendingQrToken)) {
            renderErrorState(
                    getString(R.string.me_qr_error_invalid_title),
                    getString(R.string.me_qr_empty),
                    false
            );
            return;
        }

        loadProfile(pendingQrToken);
    }

    private void retryLoadingProfile() {
        if (TextUtils.isEmpty(pendingQrToken)) {
            navigateBack();
            return;
        }
        loadProfile(pendingQrToken);
    }

    private void loadProfile(String qrToken) {
        showLoadingContent(getString(R.string.me_qr_loading));
        dmRepository.scanQrProfile(qrToken, result -> runOnUiThread(() -> {
            if (result != null && result.isSuccess() && result.getData() != null) {
                scannedUser = result.getData();
                friendRequestSent = false;
                bindProfile(scannedUser);
                return;
            }

            ScanErrorModel errorModel = resolveScanError(
                    result != null ? result.getMessage() : null
            );
            renderErrorState(errorModel.title, errorModel.message, errorModel.canRetry);
        }));
    }

    private void bindProfile(UserResponse user) {
        showContentState();

        String displayName = resolveDisplayName(user);
        String usernameHandle = formatUsername(user != null ? user.getUsername() : null);
        String userId = safeValue(user != null ? user.getId() : null);

        binding.tvDisplayName.setText(displayName);
        binding.tvUsername.setText(usernameHandle);
        binding.tvUsernameValue.setText(usernameHandle);
        binding.tvUserTag.setText(buildUserTag(userId));
        binding.tvUserId.setText(userId);
        binding.tvStatus.setText(resolveStatusLabel(user != null ? user.getStatus() : null));
        binding.tvHint.setText(R.string.me_qr_result_subtitle);

        bindAvatar(user != null ? user.getAvatarUrl() : null, displayName);
        bindStatusDot(user != null ? user.getStatus() : null);
        updateAddFriendButton();
    }

    private void updateAddFriendButton() {
        if (scannedUser == null) {
            binding.btnAddFriend.setEnabled(false);
            binding.btnAddFriend.setText(R.string.me_qr_add_friend);
            return;
        }

        if (isCurrentUser(scannedUser)) {
            binding.btnAddFriend.setEnabled(false);
            binding.btnAddFriend.setText(R.string.me_qr_you);
            return;
        }

        if (friendRequestInFlight) {
            binding.btnAddFriend.setEnabled(false);
            binding.btnAddFriend.setText(R.string.me_qr_action_loading);
            return;
        }

        if (friendRequestSent) {
            binding.btnAddFriend.setEnabled(false);
            binding.btnAddFriend.setText(R.string.me_qr_request_sent);
            return;
        }

        binding.btnAddFriend.setEnabled(true);
        binding.btnAddFriend.setText(R.string.me_qr_add_friend);
    }

    private void sendFriendRequest() {
        if (scannedUser == null || isCurrentUser(scannedUser) || friendRequestInFlight || friendRequestSent) {
            return;
        }

        String userId = scannedUser.getId();
        if (TextUtils.isEmpty(userId)) {
            showError(getString(R.string.friend_request_send_error));
            return;
        }

        friendRequestInFlight = true;
        setLoadingState(true);
        updateAddFriendButton();

        friendRepository.sendRequestById(userId, result -> runOnUiThread(() -> {
            friendRequestInFlight = false;
            setLoadingState(false);

            if (result != null && result.isSuccess()) {
                friendRequestSent = true;
                updateAddFriendButton();
                Snackbar.make(binding.getRoot(), R.string.friend_request_sent, Snackbar.LENGTH_SHORT).show();
                return;
            }

            updateAddFriendButton();
            String message = result != null ? result.getMessage() : null;
            showError(message != null ? message : getString(R.string.friend_request_send_error));
        }));
    }

    private void showLoadingContent(String message) {
        binding.layoutLoadingState.setVisibility(View.VISIBLE);
        binding.layoutErrorState.setVisibility(View.GONE);
        binding.layoutContentState.setVisibility(View.GONE);
        binding.tvLoadingMessage.setText(message);
    }

    private void showContentState() {
        binding.layoutLoadingState.setVisibility(View.GONE);
        binding.layoutErrorState.setVisibility(View.GONE);
        binding.layoutContentState.setVisibility(View.VISIBLE);
    }

    private void renderErrorState(String title, String message, boolean canRetry) {
        binding.layoutLoadingState.setVisibility(View.GONE);
        binding.layoutContentState.setVisibility(View.GONE);
        binding.layoutErrorState.setVisibility(View.VISIBLE);
        binding.tvStateTitle.setText(title);
        binding.tvStateMessage.setText(message);
        binding.btnRetry.setVisibility(canRetry ? View.VISIBLE : View.GONE);
    }

    private void bindAvatar(@Nullable String avatarUrl, @Nullable String displayName) {
        Glide.with(this).clear(binding.ivAvatar);

        String resolvedAvatarUrl = toAbsoluteAvatarUrl(avatarUrl);
        Drawable placeholder = AvatarPlaceholderUtils.createAvatarDrawable(
                this,
                displayName,
                resolveAvatarSizePx()
        );

        if (TextUtils.isEmpty(resolvedAvatarUrl)) {
            showPlaceholderAvatar(placeholder);
            return;
        }

        binding.ivAvatar.setBackground(null);
        binding.ivAvatar.setScaleType(ImageView.ScaleType.CENTER_CROP);
        Glide.with(this)
                .load(resolvedAvatarUrl)
                .transform(new CircleCrop())
                .placeholder(placeholder)
                .error(placeholder)
                .fallback(placeholder)
                .into(binding.ivAvatar);
    }

    private void showPlaceholderAvatar(@Nullable Drawable placeholder) {
        binding.ivAvatar.setBackground(null);
        binding.ivAvatar.setScaleType(ImageView.ScaleType.CENTER_CROP);
        binding.ivAvatar.setImageDrawable(placeholder);
    }

    private void bindStatusDot(@Nullable String status) {
        String normalizedStatus = status == null ? "" : status.trim().toUpperCase();
        if (TextUtils.isEmpty(normalizedStatus)) {
            binding.viewStatusDot.setVisibility(View.GONE);
            return;
        }

        int colorRes;
        switch (normalizedStatus) {
            case "ONLINE":
                colorRes = R.color.color_online;
                break;
            case "IDLE":
                colorRes = R.color.color_idle;
                break;
            case "DND":
                colorRes = R.color.color_dnd;
                break;
            default:
                colorRes = R.color.color_offline;
                break;
        }

        binding.viewStatusDot.setVisibility(View.VISIBLE);
        if (binding.viewStatusDot.getBackground() instanceof GradientDrawable) {
            GradientDrawable drawable = (GradientDrawable) binding.viewStatusDot.getBackground().mutate();
            drawable.setColor(ContextCompat.getColor(this, colorRes));
            drawable.setStroke(dpToPx(2), ContextCompat.getColor(this, android.R.color.white));
        } else {
            binding.viewStatusDot.setBackgroundTintList(
                    ColorStateList.valueOf(ContextCompat.getColor(this, colorRes))
            );
        }
    }

    private ScanErrorModel resolveScanError(@Nullable String rawMessage) {
        String normalized = rawMessage == null ? "" : rawMessage.trim().toLowerCase();

        if (normalized.contains("404") || normalized.contains("not found")) {
            return new ScanErrorModel(
                    getString(R.string.me_qr_error_not_found_title),
                    getString(R.string.me_qr_error_not_found_message),
                    true
            );
        }

        if (normalized.contains("400")
                || normalized.contains("403")
                || normalized.contains("invalid")
                || normalized.contains("expired")
                || normalized.contains("khong tai duoc ho so tu qr")) {
            return new ScanErrorModel(
                    getString(R.string.me_qr_error_invalid_title),
                    getString(R.string.me_qr_invalid),
                    true
            );
        }

        if (normalized.contains("network")
                || normalized.contains("unable to resolve host")
                || normalized.contains("timeout")
                || normalized.contains("lỗi mạng")
                || normalized.contains("loi mang")) {
            return new ScanErrorModel(
                    getString(R.string.me_qr_error_network_title),
                    getString(R.string.me_qr_error_network_message),
                    true
            );
        }

        if (!TextUtils.isEmpty(rawMessage)) {
            return new ScanErrorModel(
                    getString(R.string.me_qr_error_generic_title),
                    rawMessage,
                    true
            );
        }

        return new ScanErrorModel(
                getString(R.string.me_qr_error_generic_title),
                getString(R.string.error_generic),
                true
        );
    }

    private String extractQrToken(Intent intent) {
        if (intent == null) {
            return null;
        }

        String extraToken = intent.getStringExtra(EXTRA_QR_TOKEN);
        if (!TextUtils.isEmpty(extraToken)) {
            return extraToken;
        }

        Uri data = intent.getData();
        if (data == null) {
            return null;
        }
        return data.getQueryParameter("token");
    }

    private void navigateBack() {
        getOnBackPressedDispatcher().onBackPressed();
    }

    private boolean isCurrentUser(@Nullable UserResponse user) {
        String currentUserId = dmRepository != null ? dmRepository.getCurrentUserId() : null;
        return user != null
                && !TextUtils.isEmpty(currentUserId)
                && currentUserId.equals(user.getId());
    }

    private String resolveDisplayName(@Nullable UserResponse user) {
        if (user == null) {
            return getString(R.string.main_unknown_user);
        }

        if (!TextUtils.isEmpty(user.getDisplayName())) {
            return user.getDisplayName().trim();
        }
        if (!TextUtils.isEmpty(user.getUsername())) {
            return user.getUsername().trim();
        }
        return getString(R.string.main_unknown_user);
    }

    private String formatUsername(@Nullable String username) {
        if (TextUtils.isEmpty(username)) {
            return getString(R.string.session_unknown_value);
        }
        return "@" + username.trim();
    }

    private String buildUserTag(@Nullable String userId) {
        if (TextUtils.isEmpty(userId) || getString(R.string.session_unknown_value).equals(userId)) {
            return getString(R.string.session_unknown_value);
        }

        String compact = userId.replace("-", "").trim();
        if (compact.isEmpty()) {
            return getString(R.string.session_unknown_value);
        }

        int tagLength = Math.min(4, compact.length());
        return "#" + compact.substring(0, tagLength).toUpperCase();
    }

    private String resolveStatusLabel(@Nullable String status) {
        if (TextUtils.isEmpty(status)) {
            return getString(R.string.me_qr_status_unknown);
        }

        String normalizedStatus = status.trim().toUpperCase();
        switch (normalizedStatus) {
            case "ONLINE":
                return getString(R.string.me_qr_status_online);
            case "IDLE":
                return getString(R.string.me_qr_status_idle);
            case "DND":
                return getString(R.string.me_qr_status_dnd);
            case "OFFLINE":
                return getString(R.string.me_qr_status_offline);
            default:
                return normalizedStatus;
        }
    }

    private int resolveAvatarSizePx() {
        if (binding.ivAvatar.getLayoutParams() != null && binding.ivAvatar.getLayoutParams().width > 0) {
            return binding.ivAvatar.getLayoutParams().width;
        }
        return binding.ivAvatar.getWidth();
    }

    private int dpToPx(int value) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                value,
                getResources().getDisplayMetrics()
        ));
    }

    @Nullable
    private String toAbsoluteAvatarUrl(@Nullable String avatarUrl) {
        return NetworkConfig.resolveUrl(avatarUrl);
    }

    private String safeValue(@Nullable String value) {
        if (TextUtils.isEmpty(value)) {
            return getString(R.string.session_unknown_value);
        }
        return value.trim();
    }

    private static class ScanErrorModel {
        final String title;
        final String message;
        final boolean canRetry;

        ScanErrorModel(String title, String message, boolean canRetry) {
            this.title = title;
            this.message = message;
            this.canRetry = canRetry;
        }
    }
}
