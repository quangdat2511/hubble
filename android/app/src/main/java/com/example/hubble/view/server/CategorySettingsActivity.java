package com.example.hubble.view.server;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;

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
import com.example.hubble.databinding.ActivityCategorySettingsBinding;
import com.example.hubble.utils.TokenManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import java.util.Objects;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CategorySettingsActivity extends AppCompatActivity {

    private static final String EXTRA_SERVER_ID = "server_id";
    private static final String EXTRA_CHANNEL_ID = "channel_id";
    private static final String EXTRA_CHANNEL_NAME = "channel_name";
    private static final String EXTRA_IS_PRIVATE = "is_private";

    private ActivityCategorySettingsBinding binding;
    private String serverId;
    private String channelId;
    private boolean isPrivate;

    private String origName;

    public static Intent createIntent(Context context, String serverId,
                                       String channelId, String channelName,
                                       boolean isPrivate) {
        Intent intent = new Intent(context, CategorySettingsActivity.class);
        intent.putExtra(EXTRA_SERVER_ID, serverId);
        intent.putExtra(EXTRA_CHANNEL_ID, channelId);
        intent.putExtra(EXTRA_CHANNEL_NAME, channelName);
        intent.putExtra(EXTRA_IS_PRIVATE, isPrivate);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        super.onCreate(savedInstanceState);
        binding = ActivityCategorySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        applyEdgeToEdge();

        serverId = getIntent().getStringExtra(EXTRA_SERVER_ID);
        channelId = getIntent().getStringExtra(EXTRA_CHANNEL_ID);
        origName = getIntent().getStringExtra(EXTRA_CHANNEL_NAME);
        isPrivate = getIntent().getBooleanExtra(EXTRA_IS_PRIVATE, false);

        if (serverId == null || channelId == null) { finish(); return; }

        // Pre-warm caches for sub-screens
        ChannelPermissionsActivity.prefetch(this, serverId, channelId);
        AddChannelAccessActivity.prefetch(this, serverId, channelId);

        // Fill fields
        binding.etCategoryName.setText(origName);

        // Dirty tracking
        binding.etCategoryName.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) { updateSaveButton(); }
        });

        // Navigation
        binding.toolbar.setNavigationOnClickListener(v -> finish());
        binding.tvSave.setOnClickListener(v -> onSave());

        binding.rowPermissions.setOnClickListener(v ->
                startActivity(ChannelPermissionsActivity.createIntent(
                        this, serverId, channelId, "CATEGORY", isPrivate)));

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
        String name = getText(binding.etCategoryName);
        boolean dirty = !Objects.equals(name, origName != null ? origName : "");
        binding.tvSave.setEnabled(dirty);
        binding.tvSave.setAlpha(dirty ? 1f : 0.4f);
    }

    private void onSave() {
        String name = getText(binding.etCategoryName);
        if (name.isEmpty()) {
            Snackbar.make(binding.getRoot(),
                    R.string.category_settings_name_empty, Snackbar.LENGTH_SHORT).show();
            return;
        }

        binding.tvSave.setEnabled(false);

        UpdateChannelRequest request = new UpdateChannelRequest();
        request.setName(name);

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
                                    R.string.error_generic, Snackbar.LENGTH_SHORT).show();
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
                .setTitle(R.string.category_settings_delete_confirm_title)
                .setMessage(getString(R.string.category_settings_delete_confirm_message,
                        origName != null ? origName : ""))
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.category_settings_delete, (d, w) -> doDelete())
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
                                    R.string.error_generic, Snackbar.LENGTH_SHORT).show();
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

    private abstract static class SimpleTextWatcher implements TextWatcher {
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
    }
}
