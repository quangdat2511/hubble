package com.example.hubble.data.api;

import okhttp3.MultipartBody;
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
import com.example.hubble.data.model.auth.SessionDto;
import com.example.hubble.data.model.auth.TokenResponse;
import com.example.hubble.data.model.auth.UserCreationRequest;
import com.example.hubble.data.model.auth.UserResponse;
import com.example.hubble.data.model.dm.ChannelDto;
import com.example.hubble.data.model.dm.CreateMessageRequest;
import com.example.hubble.data.model.dm.FriendRequestResponse;
import com.example.hubble.data.model.dm.FriendUserDto;
import com.example.hubble.data.model.dm.MessageDto;
import com.example.hubble.data.model.dm.UploadResponse;

import com.example.hubble.data.model.dm.UpdateMessageRequest;
import com.example.hubble.data.model.me.UpdateProfileRequest;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Multipart;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {
    @POST("api/users/sync")
    Call<ApiResponse<UserResponse>> syncUser(
            @Header("Authorization") String token,
            @Body UserCreationRequest request
    );

    @GET("api/sessions")
    Call<ApiResponse<java.util.List<SessionDto>>> getActiveSessions(@Header("Authorization") String token);

    @DELETE("api/sessions/{sessionId}")
    Call<ApiResponse<String>> revokeSession(@Header("Authorization") String token, @Path("sessionId") String sessionId);

    @POST("api/auth/logout")
    Call<ApiResponse<String>> logout(@Body RefreshTokenRequest request);

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

    @GET("api/friends/search")
    Call<ApiResponse<java.util.List<FriendUserDto>>> searchUsers(
            @Header("Authorization") String token,
            @Query("q") String query
    );

    @POST("api/friends/requests/username/{username}")
    Call<ApiResponse<FriendRequestResponse>> sendFriendRequestByUsername(
            @Header("Authorization") String token,
            @Path("username") String username
    );

    @GET("api/friends/requests/received")
    Call<ApiResponse<java.util.List<FriendRequestResponse>>> getIncomingRequests(
            @Header("Authorization") String token
    );

    @GET("api/friends/requests/sent")
    Call<ApiResponse<java.util.List<FriendRequestResponse>>> getOutgoingRequests(
            @Header("Authorization") String token
    );

    @POST("api/friends/requests/{requestId}/accept")
    Call<ApiResponse<String>> acceptRequest(
            @Header("Authorization") String token,
            @Path("requestId") String requestId
    );

    @DELETE("api/friends/requests/{requestId}")
    Call<ApiResponse<String>> declineRequest(
            @Header("Authorization") String token,
            @Path("requestId") String requestId
    );

    @GET("api/friends/blocks")
    Call<ApiResponse<java.util.List<FriendUserDto>>> getBlockedUsers(
            @Header("Authorization") String token
    );

    @POST("api/friends/blocks/{userId}")
    Call<ApiResponse<String>> blockUser(
            @Header("Authorization") String token,
            @Path("userId") String userId
    );

    @DELETE("api/friends/blocks/{userId}")
    Call<ApiResponse<String>> unblockUser(
            @Header("Authorization") String token,
            @Path("userId") String userId
    );

    @POST("/api/friends/requests/{userId}")
    Call<ApiResponse<FriendRequestResponse>> sendFriendRequest(@Header("Authorization") String token, @Path("userId") String userId);

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

    @PATCH("api/messages/{messageId}")
    Call<ApiResponse<MessageDto>> editMessage(
            @Header("Authorization") String token,
            @Path("messageId") String messageId,
            @Body UpdateMessageRequest request
    );

    @DELETE("api/messages/{messageId}")
    Call<ApiResponse<MessageDto>> unsendMessage(
            @Header("Authorization") String token,
            @Path("messageId") String messageId
    );

    @PUT("api/users/me")
    Call<ApiResponse<UserResponse>> updateProfile(
            @Header("Authorization") String token,
            @Body UpdateProfileRequest profile
    );

    @GET("api/users/me")
    Call<ApiResponse<UserResponse>> getProfile(
            @Header("Authorization") String token
    );
}