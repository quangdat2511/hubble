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
import com.example.hubble.data.model.auth.SessionDto;
import com.example.hubble.data.model.auth.TokenResponse;
import com.example.hubble.data.model.auth.UserCreationRequest;
import com.example.hubble.data.model.auth.UserResponse;
import com.example.hubble.data.model.dm.ChannelDto;
import com.example.hubble.data.model.dm.CreateMessageRequest;
import com.example.hubble.data.model.dm.FriendRequestResponse;
import com.example.hubble.data.model.dm.FriendUserDto;
import com.example.hubble.data.model.dm.MarkChannelReadRequest;
import com.example.hubble.data.model.dm.MessageDto;
import com.example.hubble.data.model.dm.PeerReadStatusDto;
import com.example.hubble.data.model.dm.ReactionDto;
import com.example.hubble.data.model.dm.UploadResponse;
import com.example.hubble.data.model.dm.UpdateMessageRequest;
import com.example.hubble.data.model.me.AvatarResponse;
import com.example.hubble.data.model.me.UpdateProfileRequest;
import com.example.hubble.data.model.settings.PushConfigRequest;
import com.example.hubble.data.model.settings.PushConfigResponse;

import java.util.List;

import okhttp3.MultipartBody;
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
    Call<ApiResponse<FriendRequestResponse>> sendFriendRequest(
            @Header("Authorization") String token,
            @Path("userId") String userId
    );

    @POST("api/auth/refresh")
    Call<ApiResponse<TokenResponse>> refreshToken(@Body RefreshTokenRequest request);

    @GET("api/settings/push")
    Call<ApiResponse<PushConfigResponse>> getPushConfig(
            @Header("Authorization") String token
    );

    @PUT("api/settings/push")
    Call<ApiResponse<PushConfigResponse>> updatePushConfig(
            @Header("Authorization") String token,
            @Body PushConfigRequest request
    );

    @GET("api/friends/friends")
    Call<ApiResponse<java.util.List<FriendUserDto>>> getFriends(
            @Header("Authorization") String token
    );

    @DELETE("api/friends/friends/{userId}")
    Call<ApiResponse<String>> unfriend(
            @Header("Authorization") String token,
            @Path("userId") String userId
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

    @POST("api/messages/channel/{channelId}/read")
    Call<ApiResponse<Object>> markChannelRead(
            @Header("Authorization") String token,
            @Path("channelId") String channelId,
            @Body MarkChannelReadRequest body
    );

    @GET("api/messages/channel/{channelId}/peer-read-status")
    Call<ApiResponse<PeerReadStatusDto>> getPeerReadStatus(
            @Header("Authorization") String token,
            @Path("channelId") String channelId
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

    @Multipart
    @POST("api/users/me/avatar")
    Call<ApiResponse<UserResponse>> uploadMyAvatar(
            @Header("Authorization") String token,
            @Part MultipartBody.Part file
    );

    @GET("api/users/me/avatar")
    Call<ApiResponse<AvatarResponse>> getMyAvatar(
            @Header("Authorization") String token
    );

    @GET("api/users/{userId}/avatar")
    Call<ApiResponse<AvatarResponse>> getUserAvatar(
            @Header("Authorization") String token,
            @Path("userId") String userId
    );

    @PUT("api/messages/{messageId}/reactions")
    Call<ApiResponse<List<ReactionDto>>> toggleReaction(
            @Header("Authorization") String token,
            @Path("messageId") String messageId,
            @Body java.util.Map<String, String> body
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
    @PUT("api/settings/theme")
    Call<ApiResponse<String>> updateTheme(
            @Header("Authorization") String token,
            @Query("theme") String theme
    );

    @GET("api/settings/theme")
    Call<ApiResponse<String>> getTheme(
            @Header("Authorization") String token
    );

    @GET("api/users/me/qr")
    Call<ApiResponse<String>> getMyQrToken(@Header("Authorization") String token);

    @GET("api/users/scan/qr")
    Call<ApiResponse<UserResponse>> scanQrProfile(
            @Header("Authorization") String token,
            @Query("token") String qrToken
    );

    @PUT("api/settings/language")
    Call<ApiResponse<String>> updateLanguage(
            @Header("Authorization") String token,
            @Query("locale") String language
    );

    @GET("api/settings/language")
    Call<ApiResponse<String>> getLanguage(
            @Header("Authorization") String token
    );


    @GET("api/notifications")
    Call<ApiResponse<java.util.List<com.example.hubble.data.model.notify.NotificationResponse>>> getNotifications(
            @Header("Authorization") String token,
            @Query("page") int page,
            @Query("size") int size
    );

    @GET("api/notifications/unread-count")
    Call<ApiResponse<Long>> getNotificationUnreadCount(
            @Header("Authorization") String token
    );

    @PATCH("api/notifications/{notificationId}/read")
    Call<ApiResponse<Void>> markNotificationRead(
            @Header("Authorization") String token,
            @Path("notificationId") String notificationId
    );

    @PATCH("api/notifications/read-all")
    Call<ApiResponse<Void>> markAllNotificationsRead(
            @Header("Authorization") String token
    );

    @POST("api/device-tokens")
    Call<ApiResponse<Void>> registerDeviceToken(
            @Header("Authorization") String token,
            @Body java.util.Map<String, String> body
    );

    @DELETE("api/device-tokens/{fcmToken}")
    Call<ApiResponse<Void>> removeDeviceToken(
            @Header("Authorization") String token,
            @Path("fcmToken") String fcmToken
    );

    // ── Status endpoints ─────────────────────────────────────────────────

    @PUT("api/users/me/status")
    Call<ApiResponse<Object>> updateStatus(
            @Header("Authorization") String token,
            @Body java.util.Map<String, String> body
    );

    @PUT("api/users/me/custom-status")
    Call<ApiResponse<Object>> updateCustomStatus(
            @Header("Authorization") String token,
            @Body java.util.Map<String, String> body
    );

    @POST("api/users/me/heartbeat")
    Call<ApiResponse<Void>> heartbeat(
            @Header("Authorization") String token
    );

    @POST("api/users/me/go-online")
    Call<ApiResponse<Void>> goOnline(
            @Header("Authorization") String token
    );

    @POST("api/users/me/go-offline")
    Call<ApiResponse<Void>> goOffline(
            @Header("Authorization") String token
    );

    // ── Context window ────────────────────────────────────────────────────

    @GET("api/messages/{channelId}/around/{messageId}")
    Call<ApiResponse<List<MessageDto>>> getMessagesAround(
            @Header("Authorization") String token,
            @Path("channelId") String channelId,
            @Path("messageId") String messageId,
            @Query("limit") int limit
    );

    @GET("api/messages/{channelId}/before/{beforeId}")
    Call<ApiResponse<List<MessageDto>>> getMessagesBefore(
            @Header("Authorization") String token,
            @Path("channelId") String channelId,
            @Path("beforeId") String beforeId,
            @Query("size") int size
    );

    // ── Search — channel scope ────────────────────────────────────────────

    @GET("api/search/channel/{channelId}/messages")
    Call<ApiResponse<com.example.hubble.data.model.search.PagedResponse<com.example.hubble.data.model.search.SearchMessageDto>>> searchChannelMessages(
            @Header("Authorization") String token,
            @Path("channelId") String channelId,
            @Query("q") String q,
            @Query("page") int page,
            @Query("size") int size
    );

    @GET("api/search/channel/{channelId}/members")
    Call<ApiResponse<List<com.example.hubble.data.model.search.SearchMemberDto>>> searchChannelMembers(
            @Header("Authorization") String token,
            @Path("channelId") String channelId,
            @Query("q") String q
    );

    @GET("api/search/channel/{channelId}/channels")
    Call<ApiResponse<List<com.example.hubble.data.model.search.SearchChannelDto>>> searchChannelChannels(
            @Header("Authorization") String token,
            @Path("channelId") String channelId,
            @Query("q") String q
    );

    @GET("api/search/channel/{channelId}/media")
    Call<ApiResponse<List<com.example.hubble.data.model.search.SearchAttachmentDto>>> searchChannelMedia(
            @Header("Authorization") String token,
            @Path("channelId") String channelId
    );

    @GET("api/search/channel/{channelId}/files")
    Call<ApiResponse<List<com.example.hubble.data.model.search.SearchAttachmentDto>>> searchChannelFiles(
            @Header("Authorization") String token,
            @Path("channelId") String channelId
    );

    @GET("api/search/channel/{channelId}/pins")
    Call<ApiResponse<List<com.example.hubble.data.model.search.SearchMessageDto>>> searchChannelPins(
            @Header("Authorization") String token,
            @Path("channelId") String channelId,
            @Query("q") String q
    );

    // ── Search — server scope ─────────────────────────────────────────────

    @GET("api/search/server/{serverId}/messages")
    Call<ApiResponse<com.example.hubble.data.model.search.PagedResponse<com.example.hubble.data.model.search.SearchMessageDto>>> searchServerMessages(
            @Header("Authorization") String token,
            @Path("serverId") String serverId,
            @Query("q") String q,
            @Query("page") int page,
            @Query("size") int size
    );

    @GET("api/search/server/{serverId}/members")
    Call<ApiResponse<List<com.example.hubble.data.model.search.SearchMemberDto>>> searchServerMembers(
            @Header("Authorization") String token,
            @Path("serverId") String serverId,
            @Query("q") String q
    );

    @GET("api/search/server/{serverId}/channels")
    Call<ApiResponse<List<com.example.hubble.data.model.search.SearchChannelDto>>> searchServerChannels(
            @Header("Authorization") String token,
            @Path("serverId") String serverId,
            @Query("q") String q
    );

    @GET("api/search/server/{serverId}/media")
    Call<ApiResponse<List<com.example.hubble.data.model.search.SearchAttachmentDto>>> searchServerMedia(
            @Header("Authorization") String token,
            @Path("serverId") String serverId
    );

    @GET("api/search/server/{serverId}/files")
    Call<ApiResponse<List<com.example.hubble.data.model.search.SearchAttachmentDto>>> searchServerFiles(
            @Header("Authorization") String token,
            @Path("serverId") String serverId
    );

    @GET("api/search/server/{serverId}/pins")
    Call<ApiResponse<List<com.example.hubble.data.model.search.SearchMessageDto>>> searchServerPins(
            @Header("Authorization") String token,
            @Path("serverId") String serverId,
            @Query("q") String q
    );

    // ── Search — DM scope ─────────────────────────────────────────────────

    @GET("api/search/dm/friends")
    Call<ApiResponse<List<com.example.hubble.data.model.search.SearchMemberDto>>> searchDmFriends(
            @Header("Authorization") String token,
            @Query("q") String q
    );

    @GET("api/search/dm/messages")
    Call<ApiResponse<com.example.hubble.data.model.search.PagedResponse<com.example.hubble.data.model.search.SearchMessageDto>>> searchDmMessages(
            @Header("Authorization") String token,
            @Query("q") String q,
            @Query("page") int page,
            @Query("size") int size
    );

    @GET("api/search/dm/media")
    Call<ApiResponse<List<com.example.hubble.data.model.search.SearchAttachmentDto>>> searchDmMedia(
            @Header("Authorization") String token
    );

    @GET("api/search/dm/files")
    Call<ApiResponse<List<com.example.hubble.data.model.search.SearchAttachmentDto>>> searchDmFiles(
            @Header("Authorization") String token
    );
}
