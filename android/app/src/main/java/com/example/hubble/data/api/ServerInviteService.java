package com.example.hubble.data.api;

import com.example.hubble.data.model.ApiResponse;
import com.example.hubble.data.model.server.CreateInviteRequest;
import com.example.hubble.data.model.server.ServerInviteResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

public interface ServerInviteService {

    @POST("api/servers/{serverId}/invites")
    Call<ApiResponse<ServerInviteResponse>> createInvite(
            @Header("Authorization") String token,
            @Path("serverId") String serverId,
            @Body CreateInviteRequest request
    );

    @GET("api/servers/{serverId}/invites")
    Call<ApiResponse<List<ServerInviteResponse>>> getServerInvites(
            @Header("Authorization") String token,
            @Path("serverId") String serverId
    );

    @GET("api/invites/me")
    Call<ApiResponse<List<ServerInviteResponse>>> getMyInvites(
            @Header("Authorization") String token
    );

    @PUT("api/invites/{inviteId}/accept")
    Call<ApiResponse<ServerInviteResponse>> acceptInvite(
            @Header("Authorization") String token,
            @Path("inviteId") String inviteId
    );

    @PUT("api/invites/{inviteId}/decline")
    Call<ApiResponse<ServerInviteResponse>> declineInvite(
            @Header("Authorization") String token,
            @Path("inviteId") String inviteId
    );
}

