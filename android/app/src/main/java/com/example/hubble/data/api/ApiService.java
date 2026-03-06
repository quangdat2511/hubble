package com.example.hubble.data.api;

import com.example.hubble.data.model.ApiResponse;
import com.example.hubble.data.model.UserCreationRequest;
import com.example.hubble.data.model.UserResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;

public interface ApiService {
    @POST("api/users/sync")
    Call<ApiResponse<UserResponse>> syncUser(
            @Header("Authorization") String token,
            @Body UserCreationRequest request
    );
}