package com.example.hubble.data.repository;

import android.content.Context;

import com.example.hubble.data.api.RetrofitClient;
import com.example.hubble.data.model.ApiResponse;
import com.example.hubble.data.model.AuthResult;
import com.example.hubble.data.model.EmailVerifyOtpRequest;
import com.example.hubble.data.model.ForgotPasswordRequest;
import com.example.hubble.data.model.GoogleLoginRequest;
import com.example.hubble.data.model.LoginRequest;
import com.example.hubble.data.model.PhoneSendOtpRequest;
import com.example.hubble.data.model.PhoneVerifyOtpRequest;
import com.example.hubble.data.model.RegisterRequest;
import com.example.hubble.data.model.ResetPasswordRequest;
import com.example.hubble.data.model.TokenResponse;
import com.example.hubble.data.model.UserResponse;
import com.example.hubble.utils.TokenManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AuthRepository {

    private final TokenManager tokenManager;

    public AuthRepository(Context context) {
        tokenManager = new TokenManager(context);
    }

    public UserResponse getCurrentUser() {
        return tokenManager.getUser();
    }

    public void loginWithEmail(String email, String password, RepositoryCallback<UserResponse> callback) {
        callback.onResult(AuthResult.loading());
        LoginRequest request = new LoginRequest(email, password);

        RetrofitClient.getApiService().loginWithEmail(request).enqueue(new Callback<ApiResponse<TokenResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<TokenResponse>> call, Response<ApiResponse<TokenResponse>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getResult() != null) {
                    TokenResponse tokenResponse = response.body().getResult();
                    tokenManager.saveTokens(tokenResponse.getAccessToken(), tokenResponse.getRefreshToken());
                    tokenManager.saveUser(tokenResponse.getUser());
                    callback.onResult(AuthResult.success(tokenResponse.getUser()));
                } else {
                    callback.onResult(AuthResult.error("Đăng nhập thất bại"));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<TokenResponse>> call, Throwable t) {
                callback.onResult(AuthResult.error("Lỗi kết nối: " + t.getMessage()));
            }
        });
    }

    public void registerWithEmail(String email, String password, String username, RepositoryCallback<String> callback) {
        callback.onResult(AuthResult.loading());
        RegisterRequest request = new RegisterRequest(username, username, email, password);

        RetrofitClient.getApiService().registerWithEmail(request).enqueue(new Callback<ApiResponse<String>>() {
            @Override
            public void onResponse(Call<ApiResponse<String>> call, Response<ApiResponse<String>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onResult(AuthResult.success(response.body().getResult()));
                } else {
                    callback.onResult(AuthResult.error("Đăng ký thất bại"));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<String>> call, Throwable t) {
                callback.onResult(AuthResult.error("Lỗi kết nối: " + t.getMessage()));
            }
        });
    }

    public void verifyEmailOtp(String email, String otpCode, RepositoryCallback<UserResponse> callback) {
        callback.onResult(AuthResult.loading());
        EmailVerifyOtpRequest request = new EmailVerifyOtpRequest(email, otpCode);

        RetrofitClient.getApiService().verifyEmailOtp(request).enqueue(new Callback<ApiResponse<TokenResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<TokenResponse>> call, Response<ApiResponse<TokenResponse>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getResult() != null) {
                    TokenResponse tokenResponse = response.body().getResult();
                    tokenManager.saveTokens(tokenResponse.getAccessToken(), tokenResponse.getRefreshToken());
                    tokenManager.saveUser(tokenResponse.getUser());
                    callback.onResult(AuthResult.success(tokenResponse.getUser()));
                } else {
                    callback.onResult(AuthResult.error("Mã OTP không hợp lệ"));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<TokenResponse>> call, Throwable t) {
                callback.onResult(AuthResult.error("Lỗi kết nối: " + t.getMessage()));
            }
        });
    }

    public void loginWithGoogle(String idToken, RepositoryCallback<UserResponse> callback) {
        callback.onResult(AuthResult.loading());
        GoogleLoginRequest request = new GoogleLoginRequest(idToken);

        RetrofitClient.getApiService().loginWithGoogle(request).enqueue(new Callback<ApiResponse<TokenResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<TokenResponse>> call, Response<ApiResponse<TokenResponse>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getResult() != null) {
                    TokenResponse tokenResponse = response.body().getResult();
                    tokenManager.saveTokens(tokenResponse.getAccessToken(), tokenResponse.getRefreshToken());
                    tokenManager.saveUser(tokenResponse.getUser());
                    callback.onResult(AuthResult.success(tokenResponse.getUser()));
                } else {
                    callback.onResult(AuthResult.error("Đăng nhập Google thất bại"));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<TokenResponse>> call, Throwable t) {
                callback.onResult(AuthResult.error("Lỗi kết nối: " + t.getMessage()));
            }
        });
    }

    public void sendPasswordResetEmail(String email, RepositoryCallback<String> callback) {
        callback.onResult(AuthResult.loading());
        ForgotPasswordRequest request = new ForgotPasswordRequest(email);

        RetrofitClient.getApiService().forgotPassword(request).enqueue(new Callback<ApiResponse<String>>() {
            @Override
            public void onResponse(Call<ApiResponse<String>> call, Response<ApiResponse<String>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onResult(AuthResult.success(response.body().getResult()));
                } else {
                    callback.onResult(AuthResult.error("Lỗi gửi email khôi phục"));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<String>> call, Throwable t) {
                callback.onResult(AuthResult.error("Lỗi kết nối: " + t.getMessage()));
            }
        });
    }

    public void resetPassword(String email, String otpCode, String newPassword, RepositoryCallback<String> callback) {
        callback.onResult(AuthResult.loading());
        ResetPasswordRequest request = new ResetPasswordRequest(email, otpCode, newPassword);

        RetrofitClient.getApiService().resetPassword(request).enqueue(new Callback<ApiResponse<String>>() {
            @Override
            public void onResponse(Call<ApiResponse<String>> call, Response<ApiResponse<String>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onResult(AuthResult.success(response.body().getResult()));
                } else {
                    callback.onResult(AuthResult.error("Lỗi đặt lại mật khẩu"));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<String>> call, Throwable t) {
                callback.onResult(AuthResult.error("Lỗi kết nối: " + t.getMessage()));
            }
        });
    }
    public void sendPhoneOtp(String phoneNumber, RepositoryCallback<String> callback) {
        callback.onResult(AuthResult.loading());
        PhoneSendOtpRequest request = new PhoneSendOtpRequest(phoneNumber);

        RetrofitClient.getApiService().sendPhoneOtp(request).enqueue(new Callback<ApiResponse<String>>() {
            @Override
            public void onResponse(Call<ApiResponse<String>> call, Response<ApiResponse<String>> response) {
                if (response.isSuccessful()) {
                    callback.onResult(AuthResult.success("OTP Sent"));
                } else {
                    callback.onResult(AuthResult.error("Lỗi gửi OTP"));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<String>> call, Throwable t) {
                callback.onResult(AuthResult.error("Lỗi kết nối: " + t.getMessage()));
            }
        });
    }

    public void verifyOtp(String phone, String code, RepositoryCallback<UserResponse> callback) {
        callback.onResult(AuthResult.loading());
        PhoneVerifyOtpRequest request = new PhoneVerifyOtpRequest(phone, code);

        RetrofitClient.getApiService().verifyPhoneOtp(request).enqueue(new Callback<ApiResponse<TokenResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<TokenResponse>> call, Response<ApiResponse<TokenResponse>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getResult() != null) {
                    TokenResponse tokenResponse = response.body().getResult();
                    tokenManager.saveTokens(tokenResponse.getAccessToken(), tokenResponse.getRefreshToken());
                    tokenManager.saveUser(tokenResponse.getUser());
                    callback.onResult(AuthResult.success(tokenResponse.getUser()));
                } else {
                    callback.onResult(AuthResult.error("Mã OTP không hợp lệ"));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<TokenResponse>> call, Throwable t) {
                callback.onResult(AuthResult.error("Lỗi kết nối: " + t.getMessage()));
            }
        });
    }


    public void logout() {
        tokenManager.clear();
    }
}