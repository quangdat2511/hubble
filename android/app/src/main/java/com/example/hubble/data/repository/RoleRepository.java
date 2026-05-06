package com.example.hubble.data.repository;

import android.content.Context;

import androidx.annotation.NonNull;

import com.example.hubble.data.api.RetrofitClient;
import com.example.hubble.data.api.RoleApiService;
import com.example.hubble.data.model.ApiResponse;
import com.example.hubble.data.model.auth.AuthResult;
import com.example.hubble.data.model.server.MemberBriefResponse;
import com.example.hubble.data.model.server.PermissionResponse;
import com.example.hubble.data.model.server.RoleDetailResponse;
import com.example.hubble.data.model.server.RoleResponse;
import com.example.hubble.utils.TokenManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RoleRepository {

    private final Context appContext;
    private final TokenManager tokenManager;

    public RoleRepository(Context context) {
        appContext = context.getApplicationContext();
        tokenManager = new TokenManager(appContext);
    }

    private String bearerToken() {
        String t = tokenManager.getAccessToken();
        return (t != null && !t.isEmpty()) ? "Bearer " + t : null;
    }

    // ── Get roles ─────────────────────────────────────────────────────────

    public void getRoles(String serverId, RepositoryCallback<List<RoleResponse>> callback) {
        callback.onResult(AuthResult.loading());
        String token = bearerToken();
        if (token == null) { callback.onResult(AuthResult.error("Bạn chưa đăng nhập")); return; }

        RetrofitClient.getRoleApiService(appContext).getRoles(token, serverId)
                .enqueue(new Callback<ApiResponse<List<RoleResponse>>>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiResponse<List<RoleResponse>>> call,
                                           @NonNull Response<ApiResponse<List<RoleResponse>>> resp) {
                        if (resp.isSuccessful() && resp.body() != null && resp.body().getResult() != null) {
                            callback.onResult(AuthResult.success(resp.body().getResult()));
                        } else {
                            callback.onResult(AuthResult.error("Không thể tải danh sách vai trò"));
                        }
                    }
                    @Override
                    public void onFailure(@NonNull Call<ApiResponse<List<RoleResponse>>> call, @NonNull Throwable t) {
                        callback.onResult(AuthResult.error("Lỗi kết nối: " + t.getMessage()));
                    }
                });
    }

    // ── Get role detail ───────────────────────────────────────────────────

    public void getRoleDetail(String serverId, String roleId,
                               RepositoryCallback<RoleDetailResponse> callback) {
        callback.onResult(AuthResult.loading());
        String token = bearerToken();
        if (token == null) { callback.onResult(AuthResult.error("Bạn chưa đăng nhập")); return; }

        RetrofitClient.getRoleApiService(appContext).getRoleDetail(token, serverId, roleId)
                .enqueue(new Callback<ApiResponse<RoleDetailResponse>>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiResponse<RoleDetailResponse>> call,
                                           @NonNull Response<ApiResponse<RoleDetailResponse>> resp) {
                        if (resp.isSuccessful() && resp.body() != null && resp.body().getResult() != null) {
                            callback.onResult(AuthResult.success(resp.body().getResult()));
                        } else {
                            callback.onResult(AuthResult.error("Không thể tải chi tiết vai trò"));
                        }
                    }
                    @Override
                    public void onFailure(@NonNull Call<ApiResponse<RoleDetailResponse>> call, @NonNull Throwable t) {
                        callback.onResult(AuthResult.error("Lỗi kết nối: " + t.getMessage()));
                    }
                });
    }

    // ── Create role ───────────────────────────────────────────────────────

    public void createRole(String serverId, String name, Integer color, String preset,
                           List<String> memberIds, RepositoryCallback<RoleResponse> callback) {
        callback.onResult(AuthResult.loading());
        String token = bearerToken();
        if (token == null) { callback.onResult(AuthResult.error("Bạn chưa đăng nhập")); return; }

        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        if (color != null) body.put("color", color);
        if (preset != null) body.put("preset", preset);
        if (memberIds != null && !memberIds.isEmpty()) body.put("memberIds", memberIds);

        RetrofitClient.getRoleApiService(appContext).createRole(token, serverId, body)
                .enqueue(new Callback<ApiResponse<RoleResponse>>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiResponse<RoleResponse>> call,
                                           @NonNull Response<ApiResponse<RoleResponse>> resp) {
                        if (resp.isSuccessful() && resp.body() != null && resp.body().getResult() != null) {
                            callback.onResult(AuthResult.success(resp.body().getResult()));
                        } else {
                            callback.onResult(AuthResult.error("Không thể tạo vai trò"));
                        }
                    }
                    @Override
                    public void onFailure(@NonNull Call<ApiResponse<RoleResponse>> call, @NonNull Throwable t) {
                        callback.onResult(AuthResult.error("Lỗi kết nối: " + t.getMessage()));
                    }
                });
    }

    // ── Update role ───────────────────────────────────────────────────────

    public void updateRole(String serverId, String roleId, Map<String, Object> fields,
                           RepositoryCallback<RoleResponse> callback) {
        callback.onResult(AuthResult.loading());
        String token = bearerToken();
        if (token == null) { callback.onResult(AuthResult.error("Bạn chưa đăng nhập")); return; }

        RetrofitClient.getRoleApiService(appContext).updateRole(token, serverId, roleId, fields)
                .enqueue(new Callback<ApiResponse<RoleResponse>>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiResponse<RoleResponse>> call,
                                           @NonNull Response<ApiResponse<RoleResponse>> resp) {
                        if (resp.isSuccessful() && resp.body() != null && resp.body().getResult() != null) {
                            callback.onResult(AuthResult.success(resp.body().getResult()));
                        } else {
                            callback.onResult(AuthResult.error("Không thể cập nhật vai trò"));
                        }
                    }
                    @Override
                    public void onFailure(@NonNull Call<ApiResponse<RoleResponse>> call, @NonNull Throwable t) {
                        callback.onResult(AuthResult.error("Lỗi kết nối: " + t.getMessage()));
                    }
                });
    }

    // ── Delete role ───────────────────────────────────────────────────────

    public void deleteRole(String serverId, String roleId, RepositoryCallback<Void> callback) {
        callback.onResult(AuthResult.loading());
        String token = bearerToken();
        if (token == null) { callback.onResult(AuthResult.error("Bạn chưa đăng nhập")); return; }

        RetrofitClient.getRoleApiService(appContext).deleteRole(token, serverId, roleId)
                .enqueue(new Callback<ApiResponse<Void>>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiResponse<Void>> call,
                                           @NonNull Response<ApiResponse<Void>> resp) {
                        if (resp.isSuccessful()) {
                            callback.onResult(AuthResult.success(null));
                        } else {
                            callback.onResult(AuthResult.error("Không thể xoá vai trò"));
                        }
                    }
                    @Override
                    public void onFailure(@NonNull Call<ApiResponse<Void>> call, @NonNull Throwable t) {
                        callback.onResult(AuthResult.error("Lỗi kết nối: " + t.getMessage()));
                    }
                });
    }

    // ── Get permissions ───────────────────────────────────────────────────

    public void getPermissions(String serverId, String roleId,
                               RepositoryCallback<List<PermissionResponse>> callback) {
        callback.onResult(AuthResult.loading());
        String token = bearerToken();
        if (token == null) { callback.onResult(AuthResult.error("Bạn chưa đăng nhập")); return; }

        RetrofitClient.getRoleApiService(appContext).getPermissions(token, serverId, roleId)
                .enqueue(new Callback<ApiResponse<List<PermissionResponse>>>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiResponse<List<PermissionResponse>>> call,
                                           @NonNull Response<ApiResponse<List<PermissionResponse>>> resp) {
                        if (resp.isSuccessful() && resp.body() != null && resp.body().getResult() != null) {
                            callback.onResult(AuthResult.success(resp.body().getResult()));
                        } else {
                            callback.onResult(AuthResult.error("Không thể tải phân quyền"));
                        }
                    }
                    @Override
                    public void onFailure(@NonNull Call<ApiResponse<List<PermissionResponse>>> call, @NonNull Throwable t) {
                        callback.onResult(AuthResult.error("Lỗi kết nối: " + t.getMessage()));
                    }
                });
    }

    // ── Update permissions ────────────────────────────────────────────────

    public void updatePermissions(String serverId, String roleId, List<String> grantedPermissions,
                                  RepositoryCallback<List<PermissionResponse>> callback) {
        callback.onResult(AuthResult.loading());
        String token = bearerToken();
        if (token == null) { callback.onResult(AuthResult.error("Bạn chưa đăng nhập")); return; }

        Map<String, Object> body = new HashMap<>();
        body.put("grantedPermissions", grantedPermissions);

        RetrofitClient.getRoleApiService(appContext).updatePermissions(token, serverId, roleId, body)
                .enqueue(new Callback<ApiResponse<List<PermissionResponse>>>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiResponse<List<PermissionResponse>>> call,
                                           @NonNull Response<ApiResponse<List<PermissionResponse>>> resp) {
                        if (resp.isSuccessful() && resp.body() != null && resp.body().getResult() != null) {
                            callback.onResult(AuthResult.success(resp.body().getResult()));
                        } else {
                            callback.onResult(AuthResult.error("Không thể cập nhật phân quyền"));
                        }
                    }
                    @Override
                    public void onFailure(@NonNull Call<ApiResponse<List<PermissionResponse>>> call, @NonNull Throwable t) {
                        callback.onResult(AuthResult.error("Lỗi kết nối: " + t.getMessage()));
                    }
                });
    }

    // ── Get members ───────────────────────────────────────────────────────

    public void getMembers(String serverId, String roleId,
                           RepositoryCallback<List<MemberBriefResponse>> callback) {
        callback.onResult(AuthResult.loading());
        String token = bearerToken();
        if (token == null) { callback.onResult(AuthResult.error("Bạn chưa đăng nhập")); return; }

        RetrofitClient.getRoleApiService(appContext).getMembers(token, serverId, roleId)
                .enqueue(new Callback<ApiResponse<List<MemberBriefResponse>>>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiResponse<List<MemberBriefResponse>>> call,
                                           @NonNull Response<ApiResponse<List<MemberBriefResponse>>> resp) {
                        if (resp.isSuccessful() && resp.body() != null && resp.body().getResult() != null) {
                            callback.onResult(AuthResult.success(resp.body().getResult()));
                        } else {
                            callback.onResult(AuthResult.error("Không thể tải danh sách thành viên"));
                        }
                    }
                    @Override
                    public void onFailure(@NonNull Call<ApiResponse<List<MemberBriefResponse>>> call, @NonNull Throwable t) {
                        callback.onResult(AuthResult.error("Lỗi kết nối: " + t.getMessage()));
                    }
                });
    }

    // ── Assign members ────────────────────────────────────────────────────

    public void assignMembers(String serverId, String roleId, List<String> memberIds,
                              RepositoryCallback<List<MemberBriefResponse>> callback) {
        callback.onResult(AuthResult.loading());
        String token = bearerToken();
        if (token == null) { callback.onResult(AuthResult.error("Bạn chưa đăng nhập")); return; }

        Map<String, Object> body = new HashMap<>();
        body.put("memberIds", memberIds);

        RetrofitClient.getRoleApiService(appContext).assignMembers(token, serverId, roleId, body)
                .enqueue(new Callback<ApiResponse<List<MemberBriefResponse>>>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiResponse<List<MemberBriefResponse>>> call,
                                           @NonNull Response<ApiResponse<List<MemberBriefResponse>>> resp) {
                        if (resp.isSuccessful() && resp.body() != null && resp.body().getResult() != null) {
                            callback.onResult(AuthResult.success(resp.body().getResult()));
                        } else {
                            callback.onResult(AuthResult.error("Không thể gán thành viên"));
                        }
                    }
                    @Override
                    public void onFailure(@NonNull Call<ApiResponse<List<MemberBriefResponse>>> call, @NonNull Throwable t) {
                        callback.onResult(AuthResult.error("Lỗi kết nối: " + t.getMessage()));
                    }
                });
    }

    // ── Remove member ─────────────────────────────────────────────────────

    public void removeMember(String serverId, String roleId, String memberId,
                             RepositoryCallback<Void> callback) {
        callback.onResult(AuthResult.loading());
        String token = bearerToken();
        if (token == null) { callback.onResult(AuthResult.error("Bạn chưa đăng nhập")); return; }

        RetrofitClient.getRoleApiService(appContext).removeMember(token, serverId, roleId, memberId)
                .enqueue(new Callback<ApiResponse<Void>>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiResponse<Void>> call,
                                           @NonNull Response<ApiResponse<Void>> resp) {
                        if (resp.isSuccessful()) {
                            callback.onResult(AuthResult.success(null));
                        } else {
                            callback.onResult(AuthResult.error("Không thể xoá thành viên"));
                        }
                    }
                    @Override
                    public void onFailure(@NonNull Call<ApiResponse<Void>> call, @NonNull Throwable t) {
                        callback.onResult(AuthResult.error("Lỗi kết nối: " + t.getMessage()));
                    }
                });
    }

    // ── My effective permissions ──────────────────────────────────────────

    public void loadMyPermissions(String serverId, RepositoryCallback<Set<String>> callback) {
        callback.onResult(AuthResult.loading());
        String token = bearerToken();
        if (token == null) { callback.onResult(AuthResult.error("Bạn chưa đăng nhập")); return; }

        RetrofitClient.getRoleApiService(appContext).getMyPermissions(token, serverId)
                .enqueue(new Callback<ApiResponse<Set<String>>>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiResponse<Set<String>>> call,
                                           @NonNull Response<ApiResponse<Set<String>>> resp) {
                        if (resp.isSuccessful() && resp.body() != null && resp.body().getResult() != null) {
                            callback.onResult(AuthResult.success(new HashSet<>(resp.body().getResult())));
                        } else {
                            callback.onResult(AuthResult.success(new HashSet<>()));
                        }
                    }
                    @Override
                    public void onFailure(@NonNull Call<ApiResponse<Set<String>>> call, @NonNull Throwable t) {
                        callback.onResult(AuthResult.success(new HashSet<>()));
                    }
                });
    }

    // ── Get member roles ──────────────────────────────────────────────────

    public void getMemberRoles(String serverId, String userId, RepositoryCallback<List<RoleResponse>> callback) {
        String token = bearerToken();
        if (token == null) { callback.onResult(AuthResult.error("Bạn chưa đăng nhập")); return; }

        RetrofitClient.getRoleApiService(appContext).getMemberRoles(token, serverId, userId)
                .enqueue(new Callback<ApiResponse<List<RoleResponse>>>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiResponse<List<RoleResponse>>> call,
                                           @NonNull Response<ApiResponse<List<RoleResponse>>> resp) {
                        if (resp.isSuccessful() && resp.body() != null && resp.body().getResult() != null) {
                            callback.onResult(AuthResult.success(resp.body().getResult()));
                        } else {
                            callback.onResult(AuthResult.success(List.of()));
                        }
                    }
                    @Override
                    public void onFailure(@NonNull Call<ApiResponse<List<RoleResponse>>> call, @NonNull Throwable t) {
                        callback.onResult(AuthResult.success(List.of()));
                    }
                });
    }
}
