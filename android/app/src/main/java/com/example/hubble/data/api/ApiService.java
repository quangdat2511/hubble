package com.example.hubble.data.api;

import com.example.hubble.data.model.ApiResponse;
import com.example.hubble.data.model.LoginRequest;
import com.example.hubble.data.model.PhoneLoginRequest;
import com.example.hubble.data.model.PhoneSendOtpRequest;
import com.example.hubble.data.model.PhoneVerifyOtpRequest;
import com.example.hubble.data.model.RegisterRequest;
import com.example.hubble.data.model.TokenResponse;
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

    @POST("api/auth/login")
    Call<ApiResponse<TokenResponse>> loginWithEmail(@Body LoginRequest request);

    @POST("api/auth/register")
    Call<ApiResponse<TokenResponse>> registerWithEmail(@Body RegisterRequest request);

    @POST("api/auth/login/phone")
    Call<ApiResponse<TokenResponse>> loginWithPhone(@Body PhoneLoginRequest request);

    @POST("api/auth/phone/send-otp")
    Call<ApiResponse<String>> sendPhoneOtp(@Body PhoneSendOtpRequest request);

    @POST("api/auth/phone/verify")
    Call<ApiResponse<TokenResponse>> verifyPhoneOtp(@Body PhoneVerifyOtpRequest request);
}