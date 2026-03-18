package com.example.hubble.data.api;

import com.example.hubble.data.model.ApiResponse;
import com.example.hubble.data.model.CreateServerRequest;
import com.example.hubble.data.model.ServerResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;

public interface ServerService {

    @POST("api/servers")
    Call<ApiResponse<ServerResponse>> createServer(
            @Header("Authorization") String token,
            @Body CreateServerRequest request
    );

    @GET("api/servers/me")
    Call<ApiResponse<List<ServerResponse>>> getMyServers(
            @Header("Authorization") String token
    );
}
