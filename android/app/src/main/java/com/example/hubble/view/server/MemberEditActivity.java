package com.example.hubble.view.server;

import android.content.Intent;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.example.hubble.R;
import com.example.hubble.data.api.NetworkConfig;
import com.example.hubble.data.model.server.RoleResponse;
import com.example.hubble.data.repository.RoleRepository;
import com.example.hubble.data.repository.ServerRepository;
import com.example.hubble.databinding.ActivityMemberEditBinding;
import com.example.hubble.utils.AvatarPlaceholderUtils;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

public class MemberEditActivity extends AppCompatActivity {

    public static final String EXTRA_USER_ID = "extra_user_id";
    public static final String EXTRA_USERNAME = "extra_username";
    public static final String EXTRA_DISPLAY_NAME = "extra_display_name";
    public static final String EXTRA_AVATAR_URL = "extra_avatar_url";
    public static final String EXTRA_AVATAR_BG_COLOR = "extra_avatar_bg_color";
    public static final String EXTRA_IS_CURRENT_USER_OWNER = "extra_is_current_user_owner";
    public static final String EXTRA_SERVER_ID = "extra_server_id";

    private ActivityMemberEditBinding binding;
    private String username;
    private String userId;
    private String serverId;
    private ServerRepository serverRepository;
    // Cached server roles for instant bottom-sheet open on repeated taps
    private List<RoleResponse> mCachedAllRoles;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        super.onCreate(savedInstanceState);
        binding = ActivityMemberEditBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Apply system bar insets: top = status bar, bottom = nav bar
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, windowInsets) -> {
            Insets bars = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return WindowInsetsCompat.CONSUMED;
        });

        serverRepository = new ServerRepository(this);

        userId = getIntent().getStringExtra(EXTRA_USER_ID);
        username = getIntent().getStringExtra(EXTRA_USERNAME);
        serverId = getIntent().getStringExtra(EXTRA_SERVER_ID);
        String displayName = getIntent().getStringExtra(EXTRA_DISPLAY_NAME);
        String avatarUrl = getIntent().getStringExtra(EXTRA_AVATAR_URL);
        boolean isCurrentUserOwner = getIntent().getBooleanExtra(EXTRA_IS_CURRENT_USER_OWNER, false);

        // Toolbar
        String displayText = (displayName != null && !displayName.isEmpty()) ? displayName : username;
        binding.toolbar.setTitle(getString(R.string.member_edit_title, displayText));
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        // Identity card: always use ivAvatar with Glide (circular placeholder for no-avatar case)
        int avatarSizePx = Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 56, getResources().getDisplayMetrics()));
        android.graphics.drawable.Drawable placeholder =
                AvatarPlaceholderUtils.createAvatarDrawable(this, displayText, avatarSizePx);
        binding.tvAvatarInitials.setVisibility(View.GONE);
        binding.ivAvatar.setVisibility(View.VISIBLE);
        Glide.with(this)
                .load(avatarUrl != null && !avatarUrl.isEmpty() ? NetworkConfig.resolveUrl(avatarUrl) : null)
                .placeholder(placeholder)
                .error(placeholder)
                .circleCrop()
                .into(binding.ivAvatar);

        // Pre-fetch server roles so EditMemberRolesBottomSheet opens without waiting for getRoles
        RoleRepository roleRepository = new RoleRepository(this);
        final String prefetchServerId = serverId;
        roleRepository.getRoles(prefetchServerId, result -> {
            mCachedAllRoles = (result.isSuccess() && result.getData() != null)
                    ? result.getData() : new ArrayList<>();
        });

        // Display name
        binding.tvDisplayName.setText(displayText);

        // Username handle
        if (username != null && !username.isEmpty()) {
            binding.tvUsernameHandle.setText(username);
            binding.tvUsernameHandle.setVisibility(View.VISIBLE);
        } else {
            binding.tvUsernameHandle.setVisibility(View.GONE);
        }

        // Nickname tap
        binding.tvNicknameHint.setOnClickListener(v ->
                Snackbar.make(binding.getRoot(), R.string.main_coming_soon, Snackbar.LENGTH_SHORT).show()
        );

        // Edit roles tap — push cached roles before opening so sheet skips getRoles fetch
        final String finalDisplayText = displayText;
        binding.rowEditRoles.setOnClickListener(v -> {
            EditMemberRolesBottomSheet.sPendingAllRoles = mCachedAllRoles;
            EditMemberRolesBottomSheet.newInstance(serverId, userId, finalDisplayText)
                    .show(getSupportFragmentManager(), "edit_member_roles");
        });

        // Danger zone - only visible for owner managing non-self members
        if (isCurrentUserOwner) {
            binding.cardDangerZone.setVisibility(View.VISIBLE);

            // Kick label
            binding.tvKickLabel.setText(getString(R.string.member_edit_kick, displayText));

            // Kick click
            binding.rowKick.setOnClickListener(v -> {
                new MaterialAlertDialogBuilder(this)
                        .setTitle(R.string.member_edit_kick_confirm_title)
                        .setMessage(getString(R.string.member_edit_kick_confirm_message, displayText))
                        .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                            serverRepository.kickMember(serverId, userId, result -> runOnUiThread(() -> {
                                if (result.isSuccess()) {
                                    Snackbar.make(binding.getRoot(),
                                            getString(R.string.member_edit_kick_success, displayText),
                                            Snackbar.LENGTH_SHORT).show();
                                    setResult(RESULT_OK);
                                    finish();
                                } else if (result.isError()) {
                                    Snackbar.make(binding.getRoot(),
                                            result.getMessage() != null ? result.getMessage() : getString(R.string.member_edit_kick_error),
                                            Snackbar.LENGTH_SHORT).show();
                                }
                            }));
                        })
                        .setNegativeButton(android.R.string.cancel, null)
                        .show();
            });

            // Transfer ownership click
            binding.rowTransferOwnership.setOnClickListener(v ->
                    new MaterialAlertDialogBuilder(this)
                            .setTitle(R.string.member_edit_transfer_confirm_title)
                            .setMessage(R.string.member_edit_transfer_confirm_message)
                            .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                                serverRepository.transferOwnership(serverId, userId, result -> runOnUiThread(() -> {
                                    if (result.isSuccess()) {
                                        Snackbar.make(binding.getRoot(),
                                                getString(R.string.member_edit_transfer_success, displayText),
                                                Snackbar.LENGTH_SHORT).show();
                                        setResult(RESULT_OK);
                                        finish();
                                    } else if (result.isError()) {
                                        Snackbar.make(binding.getRoot(),
                                                result.getMessage() != null ? result.getMessage() : getString(R.string.member_edit_transfer_error),
                                                Snackbar.LENGTH_SHORT).show();
                                    }
                                }));
                            })
                            .setNegativeButton(android.R.string.cancel, null)
                            .show()
            );
        } else {
            binding.cardDangerZone.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}

