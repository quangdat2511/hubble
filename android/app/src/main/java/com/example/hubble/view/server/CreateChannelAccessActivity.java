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
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.hubble.R;
import com.example.hubble.adapter.server.ChannelAccessAdapter;
import com.example.hubble.data.api.RetrofitClient;
import com.example.hubble.data.model.ApiResponse;
import com.example.hubble.data.model.dm.ChannelDto;
import com.example.hubble.data.model.server.CreateChannelRequest;
import com.example.hubble.data.model.server.RoleResponse;
import com.example.hubble.data.model.server.ServerMemberResponse;
import com.example.hubble.databinding.ActivityCreateChannelAccessBinding;
import com.example.hubble.utils.TokenManager;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CreateChannelAccessActivity extends AppCompatActivity {

    private static final String EXTRA_SERVER_ID = "server_id";
    private static final String EXTRA_CHANNEL_NAME = "channel_name";
    private static final String EXTRA_CHANNEL_TYPE = "channel_type";

    private ActivityCreateChannelAccessBinding binding;
    private ChannelAccessAdapter adapter;
    private String serverId;
    private String channelName;
    private String channelType;

    private List<RoleResponse> allRoles = new ArrayList<>();
    private List<ServerMemberResponse> allMembers = new ArrayList<>();

    public static Intent createIntent(Context context, String serverId,
                                      String channelName, String channelType) {
        Intent intent = new Intent(context, CreateChannelAccessActivity.class);
        intent.putExtra(EXTRA_SERVER_ID, serverId);
        intent.putExtra(EXTRA_CHANNEL_NAME, channelName);
        intent.putExtra(EXTRA_CHANNEL_TYPE, channelType);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        super.onCreate(savedInstanceState);
        binding = ActivityCreateChannelAccessBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        applyEdgeToEdge();

        serverId = getIntent().getStringExtra(EXTRA_SERVER_ID);
        channelName = getIntent().getStringExtra(EXTRA_CHANNEL_NAME);
        channelType = getIntent().getStringExtra(EXTRA_CHANNEL_TYPE);

        if (serverId == null) { finish(); return; }

        setupToolbar();
        setupRecyclerView();
        setupSearch();
        loadData();
    }

    private void setupToolbar() {
        binding.toolbar.setNavigationOnClickListener(v -> finish());
        binding.tvAction.setOnClickListener(v -> onActionClick());
    }

    private void setupRecyclerView() {
        adapter = new ChannelAccessAdapter();
        binding.rvAccessList.setLayoutManager(new LinearLayoutManager(this));
        binding.rvAccessList.setAdapter(adapter);

        adapter.setOnSelectionChangedListener((roleIds, memberIds) -> {
            boolean hasSelections = !roleIds.isEmpty() || !memberIds.isEmpty();
            binding.tvAction.setText(hasSelections
                    ? R.string.create_channel_create
                    : R.string.create_channel_skip);
        });
    }

    private void setupSearch() {
        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.filter(s.toString(), allRoles, allMembers);
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void loadData() {
        TokenManager tm = new TokenManager(this);
        String token = "Bearer " + tm.getAccessToken();

        // Load roles
        RetrofitClient.getRoleApiService(this).getRoles(token, serverId)
                .enqueue(new Callback<ApiResponse<List<RoleResponse>>>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiResponse<List<RoleResponse>>> call,
                                           @NonNull Response<ApiResponse<List<RoleResponse>>> response) {
                        if (response.isSuccessful() && response.body() != null
                                && response.body().getResult() != null) {
                            allRoles = response.body().getResult();
                            // Filter out @everyone default role
                            allRoles.removeIf(r -> Boolean.TRUE.equals(r.getIsDefault()));
                            trySetData();
                        }
                    }
                    @Override
                    public void onFailure(@NonNull Call<ApiResponse<List<RoleResponse>>> call,
                                         @NonNull Throwable t) {}
                });

        // Load members (excluding the current user — creator is auto-added by backend)
        String currentUserId = null;
        TokenManager tmUser = new TokenManager(this);
        if (tmUser.getUser() != null) currentUserId = tmUser.getUser().getId();
        final String finalCurrentUserId = currentUserId;

        RetrofitClient.getServerMemberService(this).getServerMembers(token, serverId)
                .enqueue(new Callback<ApiResponse<List<ServerMemberResponse>>>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiResponse<List<ServerMemberResponse>>> call,
                                           @NonNull Response<ApiResponse<List<ServerMemberResponse>>> response) {
                        if (response.isSuccessful() && response.body() != null
                                && response.body().getResult() != null) {
                            allMembers = response.body().getResult();
                            if (finalCurrentUserId != null) {
                                allMembers.removeIf(m -> finalCurrentUserId.equals(m.getUserId()));
                            }
                            trySetData();
                        }
                    }
                    @Override
                    public void onFailure(@NonNull Call<ApiResponse<List<ServerMemberResponse>>> call,
                                         @NonNull Throwable t) {}
                });
    }

    private boolean rolesLoaded() { return allRoles != null; }
    private boolean membersLoaded() { return allMembers != null; }

    private void trySetData() {
        if (rolesLoaded() && membersLoaded()) {
            adapter.setData(allRoles, allMembers);
        }
    }

    private void onActionClick() {
        Set<String> roleIds = adapter.getSelectedRoleIds();
        Set<String> memberIds = adapter.getSelectedMemberIds();

        CreateChannelRequest request = new CreateChannelRequest(
                channelName,
                channelType != null ? channelType : "TEXT",
                null,
                true,
                memberIds.isEmpty() ? null : new ArrayList<>(memberIds),
                roleIds.isEmpty() ? null : new ArrayList<>(roleIds)
        );

        binding.tvAction.setEnabled(false);

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
                                "Lỗi kết nối: " + t.getMessage(),
                                Snackbar.LENGTH_SHORT).show();
                    }
                });
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
}
