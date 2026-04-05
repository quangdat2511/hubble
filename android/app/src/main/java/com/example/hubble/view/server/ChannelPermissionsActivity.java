package com.example.hubble.view.server;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.hubble.R;
import com.example.hubble.data.api.RetrofitClient;
import com.example.hubble.data.model.ApiResponse;
import com.example.hubble.data.model.dm.ChannelDto;
import com.example.hubble.data.model.server.ChannelMemberResponse;
import com.example.hubble.data.model.server.ChannelRoleResponse;
import com.example.hubble.data.model.server.UpdateChannelRequest;
import com.example.hubble.databinding.ActivityChannelPermissionsBinding;
import com.example.hubble.utils.TokenManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChannelPermissionsActivity extends AppCompatActivity {

    private static final String EXTRA_SERVER_ID = "server_id";
    private static final String EXTRA_CHANNEL_ID = "channel_id";
    private static final String EXTRA_CHANNEL_TYPE = "channel_type";
    private static final String EXTRA_IS_PRIVATE = "is_private";

    private static final ConcurrentHashMap<String, List<ChannelMemberResponse>> memberCache = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, List<ChannelRoleResponse>> roleCache = new ConcurrentHashMap<>();

    private ActivityChannelPermissionsBinding binding;
    private String serverId;
    private String channelId;
    private boolean isPrivate;

    private final List<ChannelMemberResponse> members = new ArrayList<>();
    private final List<ChannelRoleResponse> roles = new ArrayList<>();
    private MemberAdapter memberAdapter;
    private RoleAdapter roleAdapter;

    private final ActivityResultLauncher<Intent> addAccessLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    loadAccessLists();
                }
            });

    public static Intent createIntent(Context context, String serverId, String channelId,
                                       String channelType, boolean isPrivate) {
        Intent intent = new Intent(context, ChannelPermissionsActivity.class);
        intent.putExtra(EXTRA_SERVER_ID, serverId);
        intent.putExtra(EXTRA_CHANNEL_ID, channelId);
        intent.putExtra(EXTRA_CHANNEL_TYPE, channelType);
        intent.putExtra(EXTRA_IS_PRIVATE, isPrivate);
        return intent;
    }

    public static void prefetch(Context context, String serverId, String channelId) {
        if (serverId == null || channelId == null) return;
        TokenManager tm = new TokenManager(context);
        String token = "Bearer " + tm.getAccessToken();
        if (!memberCache.containsKey(channelId)) {
            RetrofitClient.getServerService(context)
                    .getChannelMembers(token, serverId, channelId)
                    .enqueue(new Callback<ApiResponse<List<ChannelMemberResponse>>>() {
                        @Override
                        public void onResponse(@NonNull Call<ApiResponse<List<ChannelMemberResponse>>> call,
                                               @NonNull Response<ApiResponse<List<ChannelMemberResponse>>> response) {
                            if (response.isSuccessful() && response.body() != null
                                    && response.body().getResult() != null) {
                                memberCache.put(channelId, new ArrayList<>(response.body().getResult()));
                            }
                        }
                        @Override
                        public void onFailure(@NonNull Call<ApiResponse<List<ChannelMemberResponse>>> c,
                                             @NonNull Throwable t) {}
                    });
        }
        if (!roleCache.containsKey(channelId)) {
            RetrofitClient.getServerService(context)
                    .getChannelRoles(token, serverId, channelId)
                    .enqueue(new Callback<ApiResponse<List<ChannelRoleResponse>>>() {
                        @Override
                        public void onResponse(@NonNull Call<ApiResponse<List<ChannelRoleResponse>>> call,
                                               @NonNull Response<ApiResponse<List<ChannelRoleResponse>>> response) {
                            if (response.isSuccessful() && response.body() != null
                                    && response.body().getResult() != null) {
                                roleCache.put(channelId, new ArrayList<>(response.body().getResult()));
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
        binding = ActivityChannelPermissionsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        applyEdgeToEdge();

        serverId = getIntent().getStringExtra(EXTRA_SERVER_ID);
        channelId = getIntent().getStringExtra(EXTRA_CHANNEL_ID);
        isPrivate = getIntent().getBooleanExtra(EXTRA_IS_PRIVATE, false);

        if (serverId == null || channelId == null) { finish(); return; }

        binding.toolbar.setNavigationOnClickListener(v -> finish());

        // Private toggle
        binding.switchPrivate.setChecked(isPrivate);
        updatePrivateUI(isPrivate);

        binding.switchPrivate.setOnCheckedChangeListener((btn, checked) -> {
            isPrivate = checked;
            updatePrivateUI(checked);
            updatePrivacyOnServer(checked);
        });

        // RecyclerViews
        memberAdapter = new MemberAdapter();
        roleAdapter = new RoleAdapter();
        binding.rvMembers.setLayoutManager(new LinearLayoutManager(this));
        binding.rvMembers.setAdapter(memberAdapter);
        binding.rvRoles.setLayoutManager(new LinearLayoutManager(this));
        binding.rvRoles.setAdapter(roleAdapter);

        binding.rowAddAccess.setOnClickListener(v -> {
            addAccessLauncher.launch(
                    AddChannelAccessActivity.createIntent(this, serverId, channelId));
        });

        loadAccessLists();
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

    private void updatePrivateUI(boolean priv) {
        int vis = priv ? View.VISIBLE : View.GONE;
        binding.cardAddAccess.setVisibility(vis);
    }

    private void updatePrivacyOnServer(boolean priv) {
        TokenManager tm = new TokenManager(this);
        String token = "Bearer " + tm.getAccessToken();

        UpdateChannelRequest req = new UpdateChannelRequest();
        req.setIsPrivate(priv);

        RetrofitClient.getServerService(this)
                .updateChannel(token, serverId, channelId, req)
                .enqueue(new Callback<ApiResponse<ChannelDto>>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiResponse<ChannelDto>> call,
                                           @NonNull Response<ApiResponse<ChannelDto>> response) {
                        if (priv) loadAccessLists();
                    }
                    @Override
                    public void onFailure(@NonNull Call<ApiResponse<ChannelDto>> call,
                                         @NonNull Throwable t) {
                        Snackbar.make(binding.getRoot(),
                                getString(R.string.error_generic), Snackbar.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadAccessLists() {
        // Cache-first: show cached data immediately
        List<ChannelMemberResponse> cachedMembers = memberCache.get(channelId);
        if (cachedMembers != null) {
            members.clear();
            members.addAll(cachedMembers);
            memberAdapter.notifyDataSetChanged();
            binding.tvMembersHeader.setVisibility(members.isEmpty() ? View.GONE : View.VISIBLE);
            binding.rvMembers.setVisibility(members.isEmpty() ? View.GONE : View.VISIBLE);
        }

        List<ChannelRoleResponse> cachedRoles = roleCache.get(channelId);
        if (cachedRoles != null) {
            roles.clear();
            roles.addAll(cachedRoles);
            roleAdapter.notifyDataSetChanged();
            binding.tvRolesHeader.setVisibility(roles.isEmpty() ? View.GONE : View.VISIBLE);
            binding.rvRoles.setVisibility(roles.isEmpty() ? View.GONE : View.VISIBLE);
        }

        // Always fetch fresh in background
        TokenManager tm = new TokenManager(this);
        String token = "Bearer " + tm.getAccessToken();

        RetrofitClient.getServerService(this)
                .getChannelMembers(token, serverId, channelId)
                .enqueue(new Callback<ApiResponse<List<ChannelMemberResponse>>>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiResponse<List<ChannelMemberResponse>>> call,
                                           @NonNull Response<ApiResponse<List<ChannelMemberResponse>>> response) {
                        members.clear();
                        if (response.isSuccessful() && response.body() != null
                                && response.body().getResult() != null) {
                            members.addAll(response.body().getResult());
                            memberCache.put(channelId, new ArrayList<>(members));
                        }
                        memberAdapter.notifyDataSetChanged();
                        binding.tvMembersHeader.setVisibility(members.isEmpty() ? View.GONE : View.VISIBLE);
                        binding.rvMembers.setVisibility(members.isEmpty() ? View.GONE : View.VISIBLE);
                    }
                    @Override
                    public void onFailure(@NonNull Call<ApiResponse<List<ChannelMemberResponse>>> call,
                                         @NonNull Throwable t) {}
                });

        RetrofitClient.getServerService(this)
                .getChannelRoles(token, serverId, channelId)
                .enqueue(new Callback<ApiResponse<List<ChannelRoleResponse>>>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiResponse<List<ChannelRoleResponse>>> call,
                                           @NonNull Response<ApiResponse<List<ChannelRoleResponse>>> response) {
                        roles.clear();
                        if (response.isSuccessful() && response.body() != null
                                && response.body().getResult() != null) {
                            roles.addAll(response.body().getResult());
                            roleCache.put(channelId, new ArrayList<>(roles));
                        }
                        roleAdapter.notifyDataSetChanged();
                        binding.tvRolesHeader.setVisibility(roles.isEmpty() ? View.GONE : View.VISIBLE);
                        binding.rvRoles.setVisibility(roles.isEmpty() ? View.GONE : View.VISIBLE);
                    }
                    @Override
                    public void onFailure(@NonNull Call<ApiResponse<List<ChannelRoleResponse>>> call,
                                         @NonNull Throwable t) {}
                });
    }

    private void confirmRemoveMember(ChannelMemberResponse member) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.channel_permissions_remove_confirm_title)
                .setMessage(getString(R.string.channel_permissions_remove_member_confirm,
                        member.getDisplayName() != null ? member.getDisplayName() : member.getUsername()))
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.yes, (d, w) -> removeMember(member))
                .show();
    }

    private void removeMember(ChannelMemberResponse member) {
        TokenManager tm = new TokenManager(this);
        String token = "Bearer " + tm.getAccessToken();

        RetrofitClient.getServerService(this)
                .removeChannelMember(token, serverId, channelId, member.getUserId())
                .enqueue(new Callback<ApiResponse<Void>>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiResponse<Void>> call,
                                           @NonNull Response<ApiResponse<Void>> response) {
                        members.remove(member);
                        memberCache.put(channelId, new ArrayList<>(members));
                        memberAdapter.notifyDataSetChanged();
                        binding.tvMembersHeader.setVisibility(members.isEmpty() ? View.GONE : View.VISIBLE);
                        binding.rvMembers.setVisibility(members.isEmpty() ? View.GONE : View.VISIBLE);
                    }
                    @Override
                    public void onFailure(@NonNull Call<ApiResponse<Void>> call,
                                         @NonNull Throwable t) {}
                });
    }

    private void confirmRemoveRole(ChannelRoleResponse role) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.channel_permissions_remove_confirm_title)
                .setMessage(getString(R.string.channel_permissions_remove_role_confirm, role.getName()))
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.yes, (d, w) -> removeRole(role))
                .show();
    }

    private void removeRole(ChannelRoleResponse role) {
        TokenManager tm = new TokenManager(this);
        String token = "Bearer " + tm.getAccessToken();

        RetrofitClient.getServerService(this)
                .removeChannelRole(token, serverId, channelId, role.getRoleId())
                .enqueue(new Callback<ApiResponse<Void>>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiResponse<Void>> call,
                                           @NonNull Response<ApiResponse<Void>> response) {
                        roles.remove(role);
                        roleCache.put(channelId, new ArrayList<>(roles));
                        roleAdapter.notifyDataSetChanged();
                        binding.tvRolesHeader.setVisibility(roles.isEmpty() ? View.GONE : View.VISIBLE);
                        binding.rvRoles.setVisibility(roles.isEmpty() ? View.GONE : View.VISIBLE);
                    }
                    @Override
                    public void onFailure(@NonNull Call<ApiResponse<Void>> call,
                                         @NonNull Throwable t) {}
                });
    }

    // ── Adapters ───────────────────────────────────────────────────────────

    private class MemberAdapter extends RecyclerView.Adapter<MemberAdapter.VH> {
        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_channel_access, parent, false);
            return new VH(v);
        }
        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            ChannelMemberResponse m = members.get(position);
            h.tvName.setText(m.getDisplayName() != null ? m.getDisplayName() : m.getUsername());
            if (m.getAvatarUrl() != null && !m.getAvatarUrl().isEmpty()) {
                Glide.with(ChannelPermissionsActivity.this).load(m.getAvatarUrl())
                        .circleCrop().into(h.ivAvatar);
            } else {
                h.ivAvatar.setImageResource(R.drawable.ic_person);
            }
            h.ivRemove.setOnClickListener(v -> confirmRemoveMember(m));
        }
        @Override public int getItemCount() { return members.size(); }

        class VH extends RecyclerView.ViewHolder {
            final ShapeableImageView ivAvatar;
            final TextView tvName;
            final ImageView ivRemove;
            VH(@NonNull View itemView) {
                super(itemView);
                ivAvatar = itemView.findViewById(R.id.ivAvatar);
                tvName = itemView.findViewById(R.id.tvName);
                ivRemove = itemView.findViewById(R.id.ivRemove);
            }
        }
    }

    private class RoleAdapter extends RecyclerView.Adapter<RoleAdapter.VH> {
        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_channel_access, parent, false);
            return new VH(v);
        }
        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            ChannelRoleResponse r = roles.get(position);
            h.tvName.setText(r.getName());
            h.ivAvatar.setImageResource(R.drawable.ic_shield);
            if (r.getColor() != null && r.getColor() != 0) {
                h.ivAvatar.setColorFilter(r.getColor());
            }
            h.ivRemove.setOnClickListener(v -> confirmRemoveRole(r));
        }
        @Override public int getItemCount() { return roles.size(); }

        class VH extends RecyclerView.ViewHolder {
            final ShapeableImageView ivAvatar;
            final TextView tvName;
            final ImageView ivRemove;
            VH(@NonNull View itemView) {
                super(itemView);
                ivAvatar = itemView.findViewById(R.id.ivAvatar);
                tvName = itemView.findViewById(R.id.tvName);
                ivRemove = itemView.findViewById(R.id.ivRemove);
            }
        }
    }
}
