package com.example.hubble.data.api;

import com.example.hubble.data.model.ApiResponse;
import com.example.hubble.data.model.auth.EmailVerifyOtpRequest;
import com.example.hubble.data.model.auth.ForgotPasswordRequest;
import com.example.hubble.data.model.auth.GoogleLoginRequest;
import com.example.hubble.data.model.auth.LoginRequest;
import com.example.hubble.data.model.auth.PhoneLoginRequest;
import com.example.hubble.data.model.auth.PhoneSendOtpRequest;
import com.example.hubble.data.model.auth.PhoneVerifyOtpRequest;
import com.example.hubble.data.model.auth.RefreshTokenRequest;
import com.example.hubble.data.model.auth.RegisterRequest;
import com.example.hubble.data.model.auth.ResetPasswordRequest;
import com.example.hubble.data.model.auth.TokenResponse;
import com.example.hubble.data.model.auth.UserCreationRequest;
import com.example.hubble.data.model.auth.UserResponse;
import com.example.hubble.data.model.dm.ChannelDto;
import com.example.hubble.data.model.dm.CreateMessageRequest;
import com.example.hubble.data.model.dm.FriendUserDto;
import com.example.hubble.data.model.dm.MessageDto;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.PUT;
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

    @POST("api/auth/refresh")
    Call<ApiResponse<TokenResponse>> refreshToken(@Body RefreshTokenRequest request);

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

        @PUT("api/settings/theme")
        Call<ApiResponse<String>> updateTheme(
                @Header("Authorization") String token,
                @Query("theme") String theme
        );

        @GET("api/settings/theme")
        Call<ApiResponse<String>> getTheme(
                @Header("Authorization") String token
        );
}
