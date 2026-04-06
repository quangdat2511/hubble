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
import com.example.hubble.data.model.server.ChannelMemberResponse;
import com.example.hubble.data.model.server.ChannelRoleResponse;
import com.example.hubble.data.model.server.RoleResponse;
import com.example.hubble.data.model.server.ServerMemberResponse;
import com.example.hubble.databinding.ActivityAddChannelAccessBinding;
import com.example.hubble.utils.TokenManager;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AddChannelAccessActivity extends AppCompatActivity {

    private static final String EXTRA_SERVER_ID = "server_id";
    private static final String EXTRA_CHANNEL_ID = "channel_id";

    private ActivityAddChannelAccessBinding binding;
    private ChannelAccessAdapter adapter;
    private String serverId;
    private String channelId;

    private static final ConcurrentHashMap<String, List<RoleResponse>> serverRoleCache = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, List<ServerMemberResponse>> serverMemberCache = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, List<ChannelMemberResponse>> channelMemberCache = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, List<ChannelRoleResponse>> channelRoleCache = new ConcurrentHashMap<>();

    private List<RoleResponse> allServerRoles = new ArrayList<>();
    private List<ServerMemberResponse> allServerMembers = new ArrayList<>();
    private List<RoleResponse> availableRoles = new ArrayList<>();
    private List<ServerMemberResponse> availableMembers = new ArrayList<>();

    // IDs already on the channel — to filter out
    private final Set<String> existingMemberIds = new HashSet<>();
    private final Set<String> existingRoleIds = new HashSet<>();

    private int pendingLoads = 0;

    public static Intent createIntent(Context context, String serverId, String channelId) {
        Intent intent = new Intent(context, AddChannelAccessActivity.class);
        intent.putExtra(EXTRA_SERVER_ID, serverId);
        intent.putExtra(EXTRA_CHANNEL_ID, channelId);
        return intent;
    }

    public static void prefetch(Context context, String serverId, String channelId) {
        if (serverId == null || channelId == null) return;
        TokenManager tm = new TokenManager(context);
        String token = "Bearer " + tm.getAccessToken();
        if (!serverRoleCache.containsKey(serverId)) {
            RetrofitClient.getRoleApiService(context).getRoles(token, serverId)
                    .enqueue(new Callback<ApiResponse<List<RoleResponse>>>() {
                        @Override
                        public void onResponse(@NonNull Call<ApiResponse<List<RoleResponse>>> call,
                                               @NonNull Response<ApiResponse<List<RoleResponse>>> response) {
                            if (response.isSuccessful() && response.body() != null
                                    && response.body().getResult() != null) {
                                serverRoleCache.put(serverId, new ArrayList<>(response.body().getResult()));
                            }
                        }
                        @Override
                        public void onFailure(@NonNull Call<ApiResponse<List<RoleResponse>>> c,
                                             @NonNull Throwable t) {}
                    });
        }
        if (!serverMemberCache.containsKey(serverId)) {
            RetrofitClient.getServerMemberService(context).getServerMembers(token, serverId)
                    .enqueue(new Callback<ApiResponse<List<ServerMemberResponse>>>() {
                        @Override
                        public void onResponse(@NonNull Call<ApiResponse<List<ServerMemberResponse>>> call,
                                               @NonNull Response<ApiResponse<List<ServerMemberResponse>>> response) {
                            if (response.isSuccessful() && response.body() != null
                                    && response.body().getResult() != null) {
                                serverMemberCache.put(serverId, new ArrayList<>(response.body().getResult()));
                            }
                        }
                        @Override
                        public void onFailure(@NonNull Call<ApiResponse<List<ServerMemberResponse>>> c,
                                             @NonNull Throwable t) {}
                    });
        }
        // Channel members/roles — AddChannelAccessActivity has its own caches
        if (!channelMemberCache.containsKey(channelId)) {
            RetrofitClient.getServerService(context)
                    .getChannelMembers(token, serverId, channelId)
                    .enqueue(new Callback<ApiResponse<List<ChannelMemberResponse>>>() {
                        @Override
                        public void onResponse(@NonNull Call<ApiResponse<List<ChannelMemberResponse>>> call,
                                               @NonNull Response<ApiResponse<List<ChannelMemberResponse>>> response) {
                            if (response.isSuccessful() && response.body() != null
                                    && response.body().getResult() != null) {
                                channelMemberCache.put(channelId, new ArrayList<>(response.body().getResult()));
                            }
                        }
                        @Override
                        public void onFailure(@NonNull Call<ApiResponse<List<ChannelMemberResponse>>> c,
                                             @NonNull Throwable t) {}
                    });
        }
        if (!channelRoleCache.containsKey(channelId)) {
            RetrofitClient.getServerService(context)
                    .getChannelRoles(token, serverId, channelId)
                    .enqueue(new Callback<ApiResponse<List<ChannelRoleResponse>>>() {
                        @Override
                        public void onResponse(@NonNull Call<ApiResponse<List<ChannelRoleResponse>>> call,
                                               @NonNull Response<ApiResponse<List<ChannelRoleResponse>>> response) {
                            if (response.isSuccessful() && response.body() != null
                                    && response.body().getResult() != null) {
                                channelRoleCache.put(channelId, new ArrayList<>(response.body().getResult()));
                            }
                        }
                        @Override
                        public void onFailure(@NonNull Call<ApiResponse<List<ChannelRoleResponse>>> c,
                                             @NonNull Throwable t) {}
                    });
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        super.onCreate(savedInstanceState);
        binding = ActivityAddChannelAccessBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        applyEdgeToEdge();

        serverId = getIntent().getStringExtra(EXTRA_SERVER_ID);
        channelId = getIntent().getStringExtra(EXTRA_CHANNEL_ID);

        if (serverId == null || channelId == null) { finish(); return; }

        binding.toolbar.setNavigationOnClickListener(v -> finish());
        binding.tvAction.setOnClickListener(v -> onSave());

        adapter = new ChannelAccessAdapter();
        binding.rvAccessList.setLayoutManager(new LinearLayoutManager(this));
        binding.rvAccessList.setAdapter(adapter);

        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.filter(s.toString(), availableRoles, availableMembers);
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        loadData();
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

    private void loadData() {
        TokenManager tm = new TokenManager(this);
        String token = "Bearer " + tm.getAccessToken();

        // Cache-first: if all 4 caches warm, show immediately
        List<ChannelMemberResponse> cachedChMembers = channelMemberCache.get(channelId);
        List<ChannelRoleResponse> cachedChRoles = channelRoleCache.get(channelId);
        List<RoleResponse> cachedServerRoles = serverRoleCache.get(serverId);
        List<ServerMemberResponse> cachedServerMembers = serverMemberCache.get(serverId);

        if (cachedChMembers != null && cachedChRoles != null
                && cachedServerRoles != null && cachedServerMembers != null) {
            existingMemberIds.clear();
            for (ChannelMemberResponse m : cachedChMembers) existingMemberIds.add(m.getUserId());
            existingRoleIds.clear();
            for (ChannelRoleResponse r : cachedChRoles) existingRoleIds.add(r.getRoleId());
            allServerRoles = new ArrayList<>(cachedServerRoles);
            allServerMembers = new ArrayList<>(cachedServerMembers);
            populateList();
        }

        // Always fetch fresh in background
        existingMemberIds.clear();
        existingRoleIds.clear();
        pendingLoads = 4;

        // 1. Load existing channel members
        RetrofitClient.getServerService(this)
                .getChannelMembers(token, serverId, channelId)
                .enqueue(new Callback<ApiResponse<List<ChannelMemberResponse>>>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiResponse<List<ChannelMemberResponse>>> call,
                                           @NonNull Response<ApiResponse<List<ChannelMemberResponse>>> response) {
                        if (response.isSuccessful() && response.body() != null
                                && response.body().getResult() != null) {
                            List<ChannelMemberResponse> result = response.body().getResult();
                            for (ChannelMemberResponse m : result) {
                                existingMemberIds.add(m.getUserId());
                            }
                            channelMemberCache.put(channelId, new ArrayList<>(result));
                        }
                        checkReady();
                    }
                    @Override
                    public void onFailure(@NonNull Call<ApiResponse<List<ChannelMemberResponse>>> c,
                                         @NonNull Throwable t) { checkReady(); }
                });

        // 2. Load existing channel roles
        RetrofitClient.getServerService(this)
                .getChannelRoles(token, serverId, channelId)
                .enqueue(new Callback<ApiResponse<List<ChannelRoleResponse>>>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiResponse<List<ChannelRoleResponse>>> call,
                                           @NonNull Response<ApiResponse<List<ChannelRoleResponse>>> response) {
                        if (response.isSuccessful() && response.body() != null
                                && response.body().getResult() != null) {
                            List<ChannelRoleResponse> result = response.body().getResult();
                            for (ChannelRoleResponse r : result) {
                                existingRoleIds.add(r.getRoleId());
                            }
                            channelRoleCache.put(channelId, new ArrayList<>(result));
                        }
                        checkReady();
                    }
                    @Override
                    public void onFailure(@NonNull Call<ApiResponse<List<ChannelRoleResponse>>> c,
                                         @NonNull Throwable t) { checkReady(); }
                });

        // 3. Load all server roles
        RetrofitClient.getRoleApiService(this).getRoles(token, serverId)
                .enqueue(new Callback<ApiResponse<List<RoleResponse>>>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiResponse<List<RoleResponse>>> call,
                                           @NonNull Response<ApiResponse<List<RoleResponse>>> response) {
                        if (response.isSuccessful() && response.body() != null
                                && response.body().getResult() != null) {
                            allServerRoles = new ArrayList<>(response.body().getResult());
                            serverRoleCache.put(serverId, new ArrayList<>(allServerRoles));
                        }
                        checkReady();
                    }
                    @Override
                    public void onFailure(@NonNull Call<ApiResponse<List<RoleResponse>>> c,
                                         @NonNull Throwable t) { checkReady(); }
                });

        // 4. Load all server members
        RetrofitClient.getServerMemberService(this).getServerMembers(token, serverId)
                .enqueue(new Callback<ApiResponse<List<ServerMemberResponse>>>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiResponse<List<ServerMemberResponse>>> call,
                                           @NonNull Response<ApiResponse<List<ServerMemberResponse>>> response) {
                        if (response.isSuccessful() && response.body() != null
                                && response.body().getResult() != null) {
                            allServerMembers = new ArrayList<>(response.body().getResult());
                            serverMemberCache.put(serverId, new ArrayList<>(allServerMembers));
                        }
                        checkReady();
                    }
                    @Override
                    public void onFailure(@NonNull Call<ApiResponse<List<ServerMemberResponse>>> c,
                                         @NonNull Throwable t) { checkReady(); }
                });
    }

    private synchronized void checkReady() {
        pendingLoads--;
        if (pendingLoads <= 0) {
            runOnUiThread(this::populateList);
        }
    }

    private void populateList() {
        // Derive filtered lists from raw data (non-mutating, safe to call multiple times)
        availableRoles = new ArrayList<>();
        for (RoleResponse r : allServerRoles) {
            if (!Boolean.TRUE.equals(r.getIsDefault()) && !existingRoleIds.contains(r.getId())) {
                availableRoles.add(r);
            }
        }
        availableMembers = new ArrayList<>();
        for (ServerMemberResponse m : allServerMembers) {
            if (!existingMemberIds.contains(m.getUserId())) {
                availableMembers.add(m);
            }
        }
        adapter.setData(availableRoles, availableMembers);
    }

    private void onSave() {
        Set<String> roleIds = adapter.getSelectedRoleIds();
        Set<String> memberIds = adapter.getSelectedMemberIds();

        if (roleIds.isEmpty() && memberIds.isEmpty()) {
            finish();
            return;
        }

        binding.tvAction.setEnabled(false);

        TokenManager tm = new TokenManager(this);
        String token = "Bearer " + tm.getAccessToken();

        int[] remaining = {0};
        if (!memberIds.isEmpty()) remaining[0]++;
        if (!roleIds.isEmpty()) remaining[0]++;

        Runnable checkDone = () -> {
            remaining[0]--;
            if (remaining[0] <= 0) {
                setResult(Activity.RESULT_OK);
                finish();
            }
        };

        if (!memberIds.isEmpty()) {
            RetrofitClient.getServerService(this)
                    .addChannelMembers(token, serverId, channelId, new ArrayList<>(memberIds))
                    .enqueue(new Callback<ApiResponse<Void>>() {
                        @Override
                        public void onResponse(@NonNull Call<ApiResponse<Void>> call,
                                               @NonNull Response<ApiResponse<Void>> response) {
                            checkDone.run();
                        }
                        @Override
                        public void onFailure(@NonNull Call<ApiResponse<Void>> call,
                                             @NonNull Throwable t) {
                            checkDone.run();
                        }
                    });
        }

        if (!roleIds.isEmpty()) {
            RetrofitClient.getServerService(this)
                    .addChannelRoles(token, serverId, channelId, new ArrayList<>(roleIds))
                    .enqueue(new Callback<ApiResponse<Void>>() {
                        @Override
                        public void onResponse(@NonNull Call<ApiResponse<Void>> call,
                                               @NonNull Response<ApiResponse<Void>> response) {
                            checkDone.run();
                        }
                        @Override
                        public void onFailure(@NonNull Call<ApiResponse<Void>> call,
                                             @NonNull Throwable t) {
                            checkDone.run();
                        }
                    });
        }
    }
}
