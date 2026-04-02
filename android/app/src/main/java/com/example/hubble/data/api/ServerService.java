package com.example.hubble.data.api;

import com.example.hubble.data.model.ApiResponse;
import com.example.hubble.data.model.dm.ChannelDto;
import com.example.hubble.data.model.server.ServerResponse;

import java.util.List;

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
}
