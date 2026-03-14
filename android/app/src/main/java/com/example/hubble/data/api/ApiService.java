package com.example.hubble.data.api;

import com.example.hubble.data.model.*;

import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Query;

public interface ApiService {
    @POST("api/users/sync")
    Call<ApiResponse<UserResponse>> syncUser(
            @Header("Authorization") String token,
            @Body UserCreationRequest request
    );

    @POST("api/auth/login")
    Call<ApiResponse<TokenResponse>> loginWithEmail(@Body LoginRequest request);

    @POST("api/auth/register")
    Call<ApiResponse<String>> registerWithEmail(@Body RegisterRequest request);

    @POST("api/auth/email/verify")
    Call<ApiResponse<TokenResponse>> verifyEmailOtp(@Body EmailVerifyOtpRequest request);

    @POST("api/auth/login/google")
    Call<ApiResponse<TokenResponse>> loginWithGoogle(@Body GoogleLoginRequest request);

    @POST("api/auth/forgot-password")
    Call<ApiResponse<String>> forgotPassword(@Body ForgotPasswordRequest request);

    @POST("api/auth/reset-password")
    Call<ApiResponse<String>> resetPassword(@Body ResetPasswordRequest request);

    @POST("api/auth/login/phone")
    Call<ApiResponse<TokenResponse>> loginWithPhone(@Body PhoneLoginRequest request);

    @POST("api/auth/phone/send-otp")
    Call<ApiResponse<String>> sendPhoneOtp(@Body PhoneSendOtpRequest request);

    @POST("api/auth/phone/verify")
    Call<ApiResponse<TokenResponse>> verifyPhoneOtp(@Body PhoneVerifyOtpRequest request);

    @Multipart
    @POST("api/media/upload")
    Call<ApiResponse<UploadResponse>> uploadMedia(
            @Part MultipartBody.Part file,
            @Query("folder") String folder
    );
}