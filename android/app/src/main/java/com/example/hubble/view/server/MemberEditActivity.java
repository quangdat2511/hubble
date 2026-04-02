package com.example.hubble.view.server;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.hubble.R;
import com.example.hubble.data.repository.ServerRepository;
import com.example.hubble.databinding.ActivityMemberEditBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

public class MemberEditActivity extends AppCompatActivity {

    public static final String EXTRA_USER_ID = "extra_user_id";
    public static final String EXTRA_USERNAME = "extra_username";
    public static final String EXTRA_DISPLAY_NAME = "extra_display_name";
    public static final String EXTRA_AVATAR_BG_COLOR = "extra_avatar_bg_color";
    public static final String EXTRA_IS_CURRENT_USER_OWNER = "extra_is_current_user_owner";
    public static final String EXTRA_SERVER_ID = "extra_server_id";

    private ActivityMemberEditBinding binding;
    private String username;
    private String userId;
    private String serverId;
    private ServerRepository serverRepository;

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
        int avatarBgColor = getIntent().getIntExtra(EXTRA_AVATAR_BG_COLOR, 0xFF5865F2);
        boolean isCurrentUserOwner = getIntent().getBooleanExtra(EXTRA_IS_CURRENT_USER_OWNER, false);

        // Toolbar
        String displayText = (displayName != null && !displayName.isEmpty()) ? displayName : username;
        binding.toolbar.setTitle(getString(R.string.member_edit_title, displayText));
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        // Identity card: avatar initials
        binding.tvAvatarInitials.setVisibility(View.VISIBLE);
        binding.ivAvatar.setVisibility(View.GONE);
        String initials = (displayText != null && !displayText.isEmpty())
                ? displayText.substring(0, 1).toUpperCase() : "?";
        binding.tvAvatarInitials.setText(initials);
        binding.tvAvatarInitials.setBackgroundColor(avatarBgColor);

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

        // Edit roles tap
        binding.rowEditRoles.setOnClickListener(v ->
                Snackbar.make(binding.getRoot(), R.string.main_coming_soon, Snackbar.LENGTH_SHORT).show()
        );

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
                                            result.getMessage() != null ? result.getMessage() : "Lỗi khi đuổi thành viên",
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
                                                result.getMessage() != null ? result.getMessage() : "Lỗi khi chuyển quyền sở hữu",
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

