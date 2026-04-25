package com.example.hubble.view.server;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.hubble.R;
import com.example.hubble.data.api.RetrofitClient;
import com.example.hubble.data.model.ApiResponse;
import com.example.hubble.data.model.dm.ChannelDto;
import com.example.hubble.data.model.server.UpdateChannelRequest;
import com.example.hubble.databinding.ActivityChannelSettingsBinding;
import com.example.hubble.utils.TokenManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import java.util.List;
import java.util.Objects;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChannelSettingsActivity extends AppCompatActivity {

    private static final String EXTRA_SERVER_ID = "server_id";
    private static final String EXTRA_CHANNEL_ID = "channel_id";
    private static final String EXTRA_CHANNEL_NAME = "channel_name";
    private static final String EXTRA_CHANNEL_TYPE = "channel_type";
    private static final String EXTRA_CHANNEL_TOPIC = "channel_topic";
    private static final String EXTRA_CHANNEL_PARENT_ID = "channel_parent_id";
    private static final String EXTRA_CHANNEL_PARENT_NAME = "channel_parent_name";
    private static final String EXTRA_CHANNEL_IS_PRIVATE = "channel_is_private";

    private ActivityChannelSettingsBinding binding;
    private String serverId;
    private String channelId;
    private String channelType;

    // Original values for dirty tracking
    private String origName;
    private String origTopic;
    private String origParentId;
    private String origParentName;

    // Current values
    private String currentParentId;
    private String currentParentName;

    private final ActivityResultLauncher<Intent> changeCategoryLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    currentParentId = result.getData().getStringExtra("selected_category_id");
                    currentParentName = result.getData().getStringExtra("selected_category_name");
                    binding.tvCategoryName.setText(
                            currentParentName != null ? currentParentName
                                    : getString(R.string.channel_settings_uncategorized));
                    updateSaveButton();
                }
            });

    public static Intent createIntent(Context context, String serverId, String channelId,
                                       String channelName, String channelType, String topic,
                                       String parentId, String parentName, boolean isPrivate) {
        Intent intent = new Intent(context, ChannelSettingsActivity.class);
        intent.putExtra(EXTRA_SERVER_ID, serverId);
        intent.putExtra(EXTRA_CHANNEL_ID, channelId);
        intent.putExtra(EXTRA_CHANNEL_NAME, channelName);
        intent.putExtra(EXTRA_CHANNEL_TYPE, channelType);
        intent.putExtra(EXTRA_CHANNEL_TOPIC, topic);
        intent.putExtra(EXTRA_CHANNEL_PARENT_ID, parentId);
        intent.putExtra(EXTRA_CHANNEL_PARENT_NAME, parentName);
        intent.putExtra(EXTRA_CHANNEL_IS_PRIVATE, isPrivate);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        super.onCreate(savedInstanceState);
        binding = ActivityChannelSettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        applyEdgeToEdge();

        serverId = getIntent().getStringExtra(EXTRA_SERVER_ID);
        channelId = getIntent().getStringExtra(EXTRA_CHANNEL_ID);
        channelType = getIntent().getStringExtra(EXTRA_CHANNEL_TYPE);
        origName = getIntent().getStringExtra(EXTRA_CHANNEL_NAME);
        origTopic = getIntent().getStringExtra(EXTRA_CHANNEL_TOPIC);
        origParentId = getIntent().getStringExtra(EXTRA_CHANNEL_PARENT_ID);
        origParentName = getIntent().getStringExtra(EXTRA_CHANNEL_PARENT_NAME);

        if (serverId == null || channelId == null) { finish(); return; }

        // Pre-warm caches for sub-screens so data is instant when the user navigates
        ChangeCategoryActivity.prefetch(this, serverId);
        ChannelPermissionsActivity.prefetch(this, serverId, channelId);
        AddChannelAccessActivity.prefetch(this, serverId, channelId);

        currentParentId = origParentId;
        currentParentName = origParentName;

        // Fill fields
        binding.etChannelName.setText(origName);
        binding.tvCategoryName.setText(
                origParentName != null ? origParentName
                        : getString(R.string.channel_settings_uncategorized));

        // TEXT-specific UI
        boolean isText = "TEXT".equalsIgnoreCase(channelType);
        binding.layoutTopic.setVisibility(isText ? View.VISIBLE : View.GONE);
        binding.rowPinned.setVisibility(isText ? View.VISIBLE : View.GONE);
        binding.dividerPinned.setVisibility(isText ? View.VISIBLE : View.GONE);
        if (isText) {
            binding.etChannelTopic.setText(origTopic);
        }

        // Text watchers for dirty tracking
        TextWatcher watcher = new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) { updateSaveButton(); }
        };
        binding.etChannelName.addTextChangedListener(watcher);
        if (isText) binding.etChannelTopic.addTextChangedListener(watcher);

        // Navigation
        binding.toolbar.setNavigationOnClickListener(v -> finish());
        binding.tvSave.setOnClickListener(v -> onSave());

        binding.rowCategory.setOnClickListener(v -> {
            changeCategoryLauncher.launch(
                    ChangeCategoryActivity.createIntent(this, serverId, currentParentId));
        });

        binding.rowPermissions.setOnClickListener(v -> {
            boolean isPrivate = getIntent().getBooleanExtra(EXTRA_CHANNEL_IS_PRIVATE, false);
            startActivity(ChannelPermissionsActivity.createIntent(
                    this, serverId, channelId, channelType, isPrivate));
        });

        binding.rowNotifications.setOnClickListener(v -> showComingSoon());
        binding.rowPinned.setOnClickListener(v -> showComingSoon());
        binding.rowDelete.setOnClickListener(v -> confirmDelete());
    }

    private void applyEdgeToEdge() {
        final int origTop = binding.getRoot().getPaddingTop();
        final int origBottom = binding.getRoot().getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, windowInsets) -> {
            Insets bars = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            v.setPadding(v.getPaddingLeft(), origTop + bars.top,
                    v.getPaddingRight(), origBottom + bars.bottom);
            return WindowInsetsCompat.CONSUMED;
        });
    }

    private void updateSaveButton() {
        boolean dirty = false;
        String name = getText(binding.etChannelName);
        String topic = getText(binding.etChannelTopic);

        if (!Objects.equals(name, origName != null ? origName : "")) dirty = true;
        if ("TEXT".equalsIgnoreCase(channelType)
                && !Objects.equals(topic, origTopic != null ? origTopic : "")) dirty = true;
        if (!Objects.equals(currentParentId, origParentId)) dirty = true;

        binding.tvSave.setEnabled(dirty);
        binding.tvSave.setAlpha(dirty ? 1f : 0.4f);
    }

    private void onSave() {
        String name = getText(binding.etChannelName);
        if (name.isEmpty()) {
            Snackbar.make(binding.getRoot(), R.string.channel_name_required, Snackbar.LENGTH_SHORT).show();
            return;
        }

        binding.tvSave.setEnabled(false);

        UpdateChannelRequest request = new UpdateChannelRequest();
        request.setName(name);
        if ("TEXT".equalsIgnoreCase(channelType)) {
            request.setTopic(getText(binding.etChannelTopic));
        }
        request.setParentId(currentParentId);

        TokenManager tm = new TokenManager(this);
        String token = "Bearer " + tm.getAccessToken();

        RetrofitClient.getServerService(this).updateChannel(token, serverId, channelId, request)
                .enqueue(new Callback<ApiResponse<ChannelDto>>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiResponse<ChannelDto>> call,
                                           @NonNull Response<ApiResponse<ChannelDto>> response) {
                        if (response.isSuccessful()) {
                            setResult(Activity.RESULT_OK);
                            finish();
                        } else {
                            binding.tvSave.setEnabled(true);
                            Snackbar.make(binding.getRoot(),
                                    getString(R.string.error_generic), Snackbar.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<ApiResponse<ChannelDto>> call,
                                         @NonNull Throwable t) {
                        binding.tvSave.setEnabled(true);
                        Snackbar.make(binding.getRoot(),
                                getString(R.string.error_network,
                                        t.getMessage() != null ? t.getMessage() : getString(R.string.error_network_unknown)),
                                Snackbar.LENGTH_SHORT).show();
                    }
                });
    }

    private void confirmDelete() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.channel_settings_delete_confirm_title)
                .setMessage(getString(R.string.channel_settings_delete_confirm_message,
                        origName != null ? origName : ""))
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.channel_settings_delete, (d, w) -> doDelete())
                .show();
    }

    private void doDelete() {
        TokenManager tm = new TokenManager(this);
        String token = "Bearer " + tm.getAccessToken();

        RetrofitClient.getServerService(this).deleteChannel(token, serverId, channelId)
                .enqueue(new Callback<ApiResponse<Void>>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiResponse<Void>> call,
                                           @NonNull Response<ApiResponse<Void>> response) {
                        if (response.isSuccessful()) {
                            setResult(Activity.RESULT_OK);
                            finish();
                        } else {
                            Snackbar.make(binding.getRoot(),
                                    getString(R.string.error_generic), Snackbar.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<ApiResponse<Void>> call,
                                         @NonNull Throwable t) {
                        Snackbar.make(binding.getRoot(),
                                getString(R.string.error_network,
                                        t.getMessage() != null ? t.getMessage() : getString(R.string.error_network_unknown)),
                                Snackbar.LENGTH_SHORT).show();
                    }
                });
    }

    private String getText(com.google.android.material.textfield.TextInputEditText et) {
        return et != null && et.getText() != null ? et.getText().toString().trim() : "";
    }

    private void showComingSoon() {
        Snackbar.make(binding.getRoot(), R.string.main_coming_soon, Snackbar.LENGTH_SHORT).show();
    }

    /** Minimal TextWatcher with no-op defaults */
    private abstract static class SimpleTextWatcher implements TextWatcher {
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
    }
}
