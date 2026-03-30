package com.example.hubble.data.repository;

import android.content.Context;

import com.example.hubble.data.api.ApiService;
import com.example.hubble.data.api.RetrofitClient;
import com.example.hubble.data.model.ApiResponse;
import com.example.hubble.data.model.auth.AuthResult;
import com.example.hubble.data.model.auth.EmailVerifyOtpRequest;
import com.example.hubble.data.model.auth.ForgotPasswordRequest;
import com.example.hubble.data.model.auth.GoogleLoginRequest;
import com.example.hubble.data.model.auth.LoginRequest;
import com.example.hubble.data.model.auth.PhoneSendOtpRequest;
import com.example.hubble.data.model.auth.PhoneVerifyOtpRequest;
import com.example.hubble.data.model.auth.RefreshTokenRequest;
import com.example.hubble.data.model.auth.RegisterRequest;
import com.example.hubble.data.model.auth.ResetPasswordRequest;
import com.example.hubble.data.model.auth.TokenResponse;
import com.example.hubble.data.model.auth.UserResponse;
import com.example.hubble.utils.TokenManager;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AuthRepository {

    private final Gson gson = new Gson();
    private final TokenManager tokenManager;
    private final ApiService apiService;
    private final Context context;

    public AuthRepository(Context context) {
        this.context = context.getApplicationContext();
        this.tokenManager = new TokenManager(context);
        this.apiService = RetrofitClient.getApiService(context); // Khởi tạo
    }
    public UserResponse getCurrentUser() {
        return tokenManager.getUser();
    }

    public void loginWithEmail(String email, String password, RepositoryCallback<UserResponse> callback) {
        callback.onResult(AuthResult.loading());
        LoginRequest request = new LoginRequest(email, password);

        apiService.loginWithEmail(request).enqueue(new Callback<ApiResponse<TokenResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<TokenResponse>> call, Response<ApiResponse<TokenResponse>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getResult() != null) {
                    TokenResponse tokenResponse = response.body().getResult();
                    tokenManager.saveTokens(tokenResponse.getAccessToken(), tokenResponse.getRefreshToken());
                    tokenManager.saveUser(tokenResponse.getUser());
                    callback.onResult(AuthResult.success(tokenResponse.getUser()));
                } else {
                    callback.onResult(AuthResult.error(extractErrorMessage(response, "Đăng nhập thất bại")));
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

        apiService.registerWithEmail(request).enqueue(new Callback<ApiResponse<String>>() {
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

        apiService.verifyEmailOtp(request).enqueue(new Callback<ApiResponse<TokenResponse>>() {
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

        apiService.loginWithGoogle(request).enqueue(new Callback<ApiResponse<TokenResponse>>() {
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

        apiService.forgotPassword(request).enqueue(new Callback<ApiResponse<String>>() {
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

        apiService.resetPassword(request).enqueue(new Callback<ApiResponse<String>>() {
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

        apiService.sendPhoneOtp(request).enqueue(new Callback<ApiResponse<String>>() {
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

        apiService.verifyPhoneOtp(request).enqueue(new Callback<ApiResponse<TokenResponse>>() {
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
        String refreshToken = tokenManager.getRefreshToken();
        if (refreshToken != null) {
            apiService.logout(new RefreshTokenRequest(refreshToken)).enqueue(new Callback<ApiResponse<String>>() {
                @Override
                public void onResponse(Call<ApiResponse<String>> call, Response<ApiResponse<String>> response) {
                }

                @Override
                public void onFailure(Call<ApiResponse<String>> call, Throwable t) {
                }
            });
        }
        tokenManager.clear();
    }

    private <T> String extractErrorMessage(Response<T> response, String fallback) {
        if (response == null) {
            return fallback;
        }

        try {
            if (response.errorBody() != null) {
                String raw = response.errorBody().string();
                if (raw != null && !raw.trim().isEmpty()) {
                    Type type = new TypeToken<ApiResponse<Object>>() {}.getType();
                    ApiResponse<Object> apiError = gson.fromJson(raw, type);
                    if (apiError != null && apiError.getMessage() != null && !apiError.getMessage().trim().isEmpty()) {
                        return apiError.getMessage();
                    }
                }
            }
        } catch (Exception ignored) {
            // Ignore parse errors and fall back to default message.
        }

        return fallback;
    }
}