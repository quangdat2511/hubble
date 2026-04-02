package com.example.hubble.data.api;

import com.example.hubble.data.model.ApiResponse;
import com.example.hubble.data.model.server.ServerMemberResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.PUT;

public interface ServerMemberService {

    @GET("api/servers/{serverId}/members")
    Call<ApiResponse<List<ServerMemberResponse>>> getServerMembers(
            @Header("Authorization") String token,
            @Path("serverId") String serverId
    );

    @DELETE("api/servers/{serverId}/members/{memberId}")
    Call<ApiResponse<Void>> kickMember(
            @Header("Authorization") String token,
            @Path("serverId") String serverId,
            @Path("memberId") String memberId
    );

    @POST("api/servers/{serverId}/bans/{memberId}")
    Call<ApiResponse<Void>> banMember(
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
