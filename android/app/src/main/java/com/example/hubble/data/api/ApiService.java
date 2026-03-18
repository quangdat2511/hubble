package com.example.hubble.data.api;

import com.example.hubble.data.model.*;

import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Path;
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

        @GET("api/friends/friends")
        Call<ApiResponse<java.util.List<FriendUserDto>>> getFriends(
            @Header("Authorization") String token
        );

            @GET("api/contacts/friends")
            Call<ApiResponse<java.util.List<FriendUserDto>>> getFriendsViaContacts(
                @Header("Authorization") String token
            );

        @GET("api/channels/dm")
        Call<java.util.List<ChannelDto>> getDirectChannels(
            @Header("Authorization") String token
        );

        @POST("api/channels/dm/{otherUserId}")
        Call<ChannelDto> getOrCreateDirectChannel(
            @Header("Authorization") String token,
            @Path("otherUserId") String otherUserId
        );

        @GET("api/messages/{channelId}")
        Call<ApiResponse<java.util.List<MessageDto>>> getMessages(
            @Header("Authorization") String token,
            @Path("channelId") String channelId,
            @Query("page") int page,
            @Query("size") int size
        );

        @POST("api/messages")
        Call<ApiResponse<MessageDto>> sendMessage(
            @Header("Authorization") String token,
            @Body CreateMessageRequest request
        );
}