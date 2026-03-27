package com.example.hubble.data.repository;

import android.content.Context;
import android.graphics.Color;

import com.example.hubble.R;
import com.example.hubble.data.api.RetrofitClient;
import com.example.hubble.data.api.ServerMemberService;
import com.example.hubble.data.model.ApiResponse;
import com.example.hubble.data.model.AuthResult;
import com.example.hubble.data.model.server.ServerMemberItem;
import com.example.hubble.data.model.server.ServerMemberResponse;
import com.example.hubble.data.model.server.ServerRoleItem;
import com.example.hubble.utils.TokenManager;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ServerMemberRepository {
    private Context appContext;
    private TokenManager tokenManager;
    private int[] defaultColors;

    public ServerMemberRepository(Context context) {
        this.appContext = context.getApplicationContext();
        this.tokenManager = new TokenManager(appContext);
        this.defaultColors = new int[]{
                Color.parseColor("#5865F2"),
                Color.parseColor("#57F287"),
                Color.parseColor("#FEE75C"),
                Color.parseColor("#EB459E"),
                Color.parseColor("#ED4245")
        };
    }

    public void getServerMembers(String serverId, RepositoryCallback<List<ServerMemberItem>> callback) {
        callback.onResult(AuthResult.loading());

        String token = "Bearer " + tokenManager.getAccessToken();
        ServerMemberService service = RetrofitClient.getServerMemberService(appContext);

        service.getServerMembers(token, serverId).enqueue(new Callback<ApiResponse<List<ServerMemberResponse>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<ServerMemberResponse>>> call, Response<ApiResponse<List<ServerMemberResponse>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<ServerMemberResponse> responses = response.body().getResult();
                    if (responses != null) {
                        List<ServerMemberItem> members = new ArrayList<>();
                        for (int i = 0; i < responses.size(); i++) {
                            members.add(mapToMemberItem(responses.get(i), i));
                        }
                        callback.onResult(AuthResult.success(members));
                    } else {
                        callback.onResult(AuthResult.error("No members found"));
                    }
                } else {
                    callback.onResult(AuthResult.error("Failed to fetch members"));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<ServerMemberResponse>>> call, Throwable t) {
                // Fallback to mock data for development
                callback.onResult(AuthResult.success(buildMockMembers()));
            }
        });
    }

    public void kickMember(String serverId, String memberId, RepositoryCallback<Void> callback) {
        callback.onResult(AuthResult.loading());

        String token = "Bearer " + tokenManager.getAccessToken();
        ServerMemberService service = RetrofitClient.getServerMemberService(appContext);

        service.kickMember(token, serverId, memberId).enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                if (response.isSuccessful()) {
                    callback.onResult(AuthResult.success(null));
                } else {
                    callback.onResult(AuthResult.error("Failed to kick member"));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                // Mock success for development
                callback.onResult(AuthResult.success(null));
            }
        });
    }

    public void banMember(String serverId, String memberId, RepositoryCallback<Void> callback) {
        callback.onResult(AuthResult.loading());

        String token = "Bearer " + tokenManager.getAccessToken();
        ServerMemberService service = RetrofitClient.getServerMemberService(appContext);

        service.banMember(token, serverId, memberId).enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                if (response.isSuccessful()) {
                    callback.onResult(AuthResult.success(null));
                } else {
                    callback.onResult(AuthResult.error("Failed to ban member"));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                // Mock success for development
                callback.onResult(AuthResult.success(null));
            }
        });
    }

    public void transferOwnership(String serverId, String memberId, RepositoryCallback<Void> callback) {
        callback.onResult(AuthResult.loading());

        String token = "Bearer " + tokenManager.getAccessToken();
        ServerMemberService service = RetrofitClient.getServerMemberService(appContext);

        service.transferOwnership(token, serverId, memberId).enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                if (response.isSuccessful()) {
                    callback.onResult(AuthResult.success(null));
                } else {
                    callback.onResult(AuthResult.error("Failed to transfer ownership"));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                // Mock success for development
                callback.onResult(AuthResult.success(null));
            }
        });
    }

    private ServerMemberItem mapToMemberItem(ServerMemberResponse response, int colorIndex) {
        List<ServerRoleItem> roleItems = new ArrayList<>();
        if (response.getRoles() != null) {
            for (ServerMemberResponse.RoleDto roleDto : response.getRoles()) {
                int roleColor = Color.parseColor("#5865F2");
                if (roleDto.getColor() != null && !roleDto.getColor().isEmpty()) {
                    try {
                        roleColor = Color.parseColor(roleDto.getColor());
                    } catch (IllegalArgumentException e) {
                        roleColor = Color.parseColor("#5865F2");
                    }
                }
                roleItems.add(new ServerRoleItem(roleDto.getId(), roleDto.getName(), roleColor));
            }
        }

        int bgColor = defaultColors[Math.abs(response.getUsername().hashCode()) % defaultColors.length];

        return new ServerMemberItem(
                response.getUserId(),
                response.getUsername(),
                response.getDisplayName(),
                response.getAvatarUrl(),
                bgColor,
                roleItems,
                response.getStatus(),
                response.isOwner()
        );
    }

    private List<ServerMemberItem> buildMockMembers() {
        // Use the actual logged-in user as the owner so isCurrentUserOwner resolves correctly
        String currentUserId = tokenManager.getUser() != null ? tokenManager.getUser().getId() : "u1";
        String currentUsername = tokenManager.getUser() != null ? tokenManager.getUser().getUsername() : "owner_user";
        String currentDisplayName = tokenManager.getUser() != null ? tokenManager.getUser().getDisplayName() : "Server Owner";

        List<ServerRoleItem> adminRoles = new ArrayList<>();
        adminRoles.add(new ServerRoleItem("r1", "Admin", Color.parseColor("#ED4245")));

        List<ServerRoleItem> modRoles = new ArrayList<>();
        modRoles.add(new ServerRoleItem("r2", "Moderator", Color.parseColor("#FEE75C")));

        List<ServerRoleItem> memberRoles = new ArrayList<>();
        memberRoles.add(new ServerRoleItem("r3", "Member", Color.parseColor("#5865F2")));

        List<ServerMemberItem> members = new ArrayList<>();
        members.add(new ServerMemberItem(currentUserId, currentUsername, currentDisplayName,
                null, Color.parseColor("#ED4245"), adminRoles, "ONLINE", true));
        members.add(new ServerMemberItem("u2", "moderator1", "Cool Mod",
                null, Color.parseColor("#5865F2"), modRoles, "IDLE", false));
        members.add(new ServerMemberItem("u3", "regular_user", null,
                null, Color.parseColor("#57F287"), memberRoles, "ONLINE", false));
        members.add(new ServerMemberItem("u4", "away_person", "Away Person",
                null, Color.parseColor("#FEE75C"), memberRoles, "DND", false));
        members.add(new ServerMemberItem("u5", "offline_user", null,
                null, Color.parseColor("#80848E"), memberRoles, "OFFLINE", false));

        return members;
    }
}
