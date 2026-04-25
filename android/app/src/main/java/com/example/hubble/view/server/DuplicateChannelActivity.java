package com.example.hubble.view.server;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

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
import com.example.hubble.data.model.server.CreateChannelRequest;
import com.example.hubble.databinding.ActivityDuplicateChannelBinding;
import com.example.hubble.utils.TokenManager;
import com.google.android.material.snackbar.Snackbar;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DuplicateChannelActivity extends AppCompatActivity {

    private static final String EXTRA_SERVER_ID = "server_id";
    private static final String EXTRA_CHANNEL_NAME = "channel_name";
    private static final String EXTRA_CHANNEL_TYPE = "channel_type";
    private static final String EXTRA_PARENT_ID = "parent_id";
    private static final String EXTRA_IS_PRIVATE = "is_private";

    private ActivityDuplicateChannelBinding binding;
    private String serverId;
    private String channelType;
    private String parentId;
    private boolean isPrivate;

    public static Intent createIntent(Context context, String serverId, String channelName,
                                      String channelType, String parentId, boolean isPrivate) {
        Intent intent = new Intent(context, DuplicateChannelActivity.class);
        intent.putExtra(EXTRA_SERVER_ID, serverId);
        intent.putExtra(EXTRA_CHANNEL_NAME, channelName);
        intent.putExtra(EXTRA_CHANNEL_TYPE, channelType);
        intent.putExtra(EXTRA_PARENT_ID, parentId);
        intent.putExtra(EXTRA_IS_PRIVATE, isPrivate);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        super.onCreate(savedInstanceState);
        binding = ActivityDuplicateChannelBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        applyEdgeToEdge();

        serverId = getIntent().getStringExtra(EXTRA_SERVER_ID);
        String channelName = getIntent().getStringExtra(EXTRA_CHANNEL_NAME);
        channelType = getIntent().getStringExtra(EXTRA_CHANNEL_TYPE);
        parentId = getIntent().getStringExtra(EXTRA_PARENT_ID);
        isPrivate = getIntent().getBooleanExtra(EXTRA_IS_PRIVATE, false);

        if (serverId == null) { finish(); return; }

        binding.etChannelName.setText(channelName);
        binding.tvNote.setText(getString(R.string.duplicate_channel_note, channelName));

        binding.toolbar.setNavigationOnClickListener(v -> finish());
        binding.tvAction.setOnClickListener(v -> onCreateClick());
    }

    private void applyEdgeToEdge() {
        final int origTop = binding.getRoot().getPaddingTop();
        final int origBottom = binding.getRoot().getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, windowInsets) -> {
            Insets bars = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
                            | WindowInsetsCompat.Type.displayCutout());
            v.setPadding(v.getPaddingLeft(), origTop + bars.top,
                    v.getPaddingRight(), origBottom + bars.bottom);
            return WindowInsetsCompat.CONSUMED;
        });
    }

    private void onCreateClick() {
        String name = binding.etChannelName.getText() != null
                ? binding.etChannelName.getText().toString().trim() : "";
        if (name.isEmpty()) {
            Snackbar.make(binding.getRoot(), R.string.channel_name_required, Snackbar.LENGTH_SHORT).show();
            return;
        }

        binding.tvAction.setEnabled(false);

        CreateChannelRequest request = new CreateChannelRequest(
                name,
                channelType != null ? channelType : "TEXT",
                parentId,
                isPrivate,
                null,
                null
        );

        TokenManager tm = new TokenManager(this);
        String token = "Bearer " + tm.getAccessToken();

        RetrofitClient.getServerService(this).createChannel(token, serverId, request)
                .enqueue(new Callback<ApiResponse<ChannelDto>>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiResponse<ChannelDto>> call,
                                           @NonNull Response<ApiResponse<ChannelDto>> response) {
                        if (response.isSuccessful() && response.body() != null
                                && response.body().getResult() != null) {
                            setResult(Activity.RESULT_OK);
                            finish();
                        } else {
                            binding.tvAction.setEnabled(true);
                            Snackbar.make(binding.getRoot(),
                                    getString(R.string.error_generic),
                                    Snackbar.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<ApiResponse<ChannelDto>> call,
                                         @NonNull Throwable t) {
                        binding.tvAction.setEnabled(true);
                        Snackbar.make(binding.getRoot(),
                                getString(R.string.error_network,
                                        t.getMessage() != null ? t.getMessage() : getString(R.string.error_network_unknown)),
                                Snackbar.LENGTH_SHORT).show();
                    }
                });
    }
}
