package com.example.hubble.data.api;

import com.example.hubble.data.model.ApiResponse;
import com.example.hubble.data.model.dm.ChannelDto;
import com.example.hubble.data.model.server.ChannelMemberResponse;
import com.example.hubble.data.model.server.ChannelRoleResponse;
import com.example.hubble.data.model.server.CreateChannelRequest;
import com.example.hubble.data.model.server.ServerResponse;
import com.example.hubble.data.model.server.UpdateChannelRequest;

import java.util.List;
import java.util.Map;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Body;

public interface ServerService {

    // ── Create server (multipart — breaking change from JSON) ──────────────
    @Multipart
    @POST("api/servers")
    Call<ApiResponse<ServerResponse>> createServer(
            @Header("Authorization") String token,
            @Part("name") RequestBody name,
            @Part MultipartBody.Part icon   // nullable — omitted when no icon
    );

    @GET("api/servers/me")
    Call<ApiResponse<List<ServerResponse>>> getMyServers(
            @Header("Authorization") String token
    );

    @GET("api/servers/{serverId}/channels")
    Call<ApiResponse<List<ChannelDto>>> getServerChannels(
            @Header("Authorization") String token,
            @Path("serverId") String serverId
    );

    // ── Server icon management ─────────────────────────────────────────────
    @Multipart
    @PUT("api/servers/{serverId}/icon")
    Call<ApiResponse<ServerResponse>> updateServerIcon(
            @Header("Authorization") String token,
            @Path("serverId") String serverId,
            @Part MultipartBody.Part icon
    );

    @DELETE("api/servers/{serverId}/icon")
    Call<ApiResponse<ServerResponse>> deleteServerIcon(
            @Header("Authorization") String token,
            @Path("serverId") String serverId
    );

    @DELETE("api/servers/{serverId}")
    Call<ApiResponse<Void>> deleteServer(
            @Header("Authorization") String token,
            @Path("serverId") String serverId
    );

    // ── Update server info ─────────────────────────────────────────────────
    @PUT("api/servers/{serverId}")
    Call<ApiResponse<ServerResponse>> updateServer(
            @Header("Authorization") String token,
            @Path("serverId") String serverId,
            @Body Map<String, String> body
    );

    // ── Member management ──────────────────────────────────────────────────
    @DELETE("api/servers/{serverId}/members/{memberId}")
    Call<ApiResponse<Void>> kickMember(
            @Header("Authorization") String token,
            @Path("serverId") String serverId,
            @Path("memberId") String memberId
    );

    @PUT("api/servers/{serverId}/owner/{memberId}")
    Call<ApiResponse<Void>> transferOwnership(
            @Header("Authorization") String token,
            @Path("serverId") String serverId,
            @Path("memberId") String memberId
    );

    // ── Create channel ─────────────────────────────────────────────────────
    @POST("api/servers/{serverId}/channels")
    Call<ApiResponse<ChannelDto>> createChannel(
            @Header("Authorization") String token,
            @Path("serverId") String serverId,
            @Body CreateChannelRequest request
    );

    // ── Update channel ─────────────────────────────────────────────────────
    @PUT("api/servers/{serverId}/channels/{channelId}")
    Call<ApiResponse<ChannelDto>> updateChannel(
            @Header("Authorization") String token,
            @Path("serverId") String serverId,
            @Path("channelId") String channelId,
            @Body UpdateChannelRequest request
    );

    // ── Delete channel ─────────────────────────────────────────────────────
    @DELETE("api/servers/{serverId}/channels/{channelId}")
    Call<ApiResponse<Void>> deleteChannel(
            @Header("Authorization") String token,
            @Path("serverId") String serverId,
            @Path("channelId") String channelId
    );

    // ── Channel members ────────────────────────────────────────────────────
    @GET("api/servers/{serverId}/channels/{channelId}/members")
    Call<ApiResponse<List<ChannelMemberResponse>>> getChannelMembers(
            @Header("Authorization") String token,
            @Path("serverId") String serverId,
            @Path("channelId") String channelId
    );

    @POST("api/servers/{serverId}/channels/{channelId}/members")
    Call<ApiResponse<Void>> addChannelMembers(
            @Header("Authorization") String token,
            @Path("serverId") String serverId,
            @Path("channelId") String channelId,
            @Body List<String> userIds
    );

    @DELETE("api/servers/{serverId}/channels/{channelId}/members/{userId}")
    Call<ApiResponse<Void>> removeChannelMember(
            @Header("Authorization") String token,
            @Path("serverId") String serverId,
            @Path("channelId") String channelId,
            @Path("userId") String userId
    );

    // ── Channel roles ──────────────────────────────────────────────────────
    @GET("api/servers/{serverId}/channels/{channelId}/roles")
    Call<ApiResponse<List<ChannelRoleResponse>>> getChannelRoles(
            @Header("Authorization") String token,
            @Path("serverId") String serverId,
            @Path("channelId") String channelId
    );

    @POST("api/servers/{serverId}/channels/{channelId}/roles")
    Call<ApiResponse<Void>> addChannelRoles(
            @Header("Authorization") String token,
            @Path("serverId") String serverId,
            @Path("channelId") String channelId,
            @Body List<String> roleIds
    );

    @DELETE("api/servers/{serverId}/channels/{channelId}/roles/{roleId}")
    Call<ApiResponse<Void>> removeChannelRole(
            @Header("Authorization") String token,
            @Path("serverId") String serverId,
            @Path("channelId") String channelId,
            @Path("roleId") String roleId
    );
}
