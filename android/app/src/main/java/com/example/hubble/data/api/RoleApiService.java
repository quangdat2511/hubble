package com.example.hubble.data.api;

import com.example.hubble.data.model.ApiResponse;
import com.example.hubble.data.model.server.RoleDetailResponse;
import com.example.hubble.data.model.server.RoleResponse;
import com.example.hubble.data.model.server.MemberBriefResponse;
import com.example.hubble.data.model.server.PermissionResponse;

import java.util.List;
import java.util.Map;
import java.util.Set;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

public interface RoleApiService {

    @GET("api/servers/{serverId}/roles")
    Call<ApiResponse<List<RoleResponse>>> getRoles(
            @Header("Authorization") String token,
            @Path("serverId") String serverId
    );

    @GET("api/servers/{serverId}/roles/{roleId}")
    Call<ApiResponse<RoleDetailResponse>> getRoleDetail(
            @Header("Authorization") String token,
            @Path("serverId") String serverId,
            @Path("roleId") String roleId
    );

    @POST("api/servers/{serverId}/roles")
    Call<ApiResponse<RoleResponse>> createRole(
            @Header("Authorization") String token,
            @Path("serverId") String serverId,
            @Body Map<String, Object> body
    );

    @PUT("api/servers/{serverId}/roles/{roleId}")
    Call<ApiResponse<RoleResponse>> updateRole(
            @Header("Authorization") String token,
            @Path("serverId") String serverId,
            @Path("roleId") String roleId,
            @Body Map<String, Object> body
    );

    @DELETE("api/servers/{serverId}/roles/{roleId}")
    Call<ApiResponse<Void>> deleteRole(
            @Header("Authorization") String token,
            @Path("serverId") String serverId,
            @Path("roleId") String roleId
    );

    @PUT("api/servers/{serverId}/roles/reorder")
    Call<ApiResponse<List<RoleResponse>>> reorderRoles(
            @Header("Authorization") String token,
            @Path("serverId") String serverId,
            @Body Map<String, Object> body
    );

    @GET("api/servers/{serverId}/roles/{roleId}/permissions")
    Call<ApiResponse<List<PermissionResponse>>> getPermissions(
            @Header("Authorization") String token,
            @Path("serverId") String serverId,
            @Path("roleId") String roleId
    );

    @PUT("api/servers/{serverId}/roles/{roleId}/permissions")
    Call<ApiResponse<List<PermissionResponse>>> updatePermissions(
            @Header("Authorization") String token,
            @Path("serverId") String serverId,
            @Path("roleId") String roleId,
            @Body Map<String, Object> body
    );

    @GET("api/servers/{serverId}/roles/{roleId}/members")
    Call<ApiResponse<List<MemberBriefResponse>>> getMembers(
            @Header("Authorization") String token,
            @Path("serverId") String serverId,
            @Path("roleId") String roleId
    );

    @POST("api/servers/{serverId}/roles/{roleId}/members")
    Call<ApiResponse<List<MemberBriefResponse>>> assignMembers(
            @Header("Authorization") String token,
            @Path("serverId") String serverId,
            @Path("roleId") String roleId,
            @Body Map<String, Object> body
    );

    @DELETE("api/servers/{serverId}/roles/{roleId}/members/{memberId}")
    Call<ApiResponse<Void>> removeMember(
            @Header("Authorization") String token,
            @Path("serverId") String serverId,
            @Path("roleId") String roleId,
            @Path("memberId") String memberId
    );

    @GET("api/servers/{serverId}/roles/my-permissions")
    Call<ApiResponse<Set<String>>> getMyPermissions(
            @Header("Authorization") String token,
            @Path("serverId") String serverId
    );

    @GET("api/servers/{serverId}/roles/members/{userId}")
    Call<ApiResponse<List<RoleResponse>>> getMemberRoles(
            @Header("Authorization") String token,
            @Path("serverId") String serverId,
            @Path("userId") String userId
    );
}
