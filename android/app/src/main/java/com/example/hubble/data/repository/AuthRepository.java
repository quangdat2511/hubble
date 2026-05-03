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
import com.example.hubble.data.model.auth.SendEmailOtpRequest;
import com.example.hubble.data.model.auth.TokenResponse;
import com.example.hubble.data.model.auth.UserResponse;
import com.example.hubble.utils.ThemeSyncManager;
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
        this.tokenManager = new TokenManager(this.context);
        this.apiService = RetrofitClient.getApiService(this.context);
    }

    public UserResponse getCurrentUser() {
        return tokenManager.getUser();
    }

    public void loginWithEmail(String email, String password, RepositoryCallback<UserResponse> callback) {
        callback.onResult(AuthResult.loading());
        LoginRequest request = new LoginRequest(email, password);

        apiService.loginWithEmail(request).enqueue(new Callback<ApiResponse<TokenResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<TokenResponse>> call,
                                   Response<ApiResponse<TokenResponse>> response) {
                android.util.Log.d("AuthRepository", "Login response - Code: " + (response != null ? response.code() : "null") + ", isSuccessful: " + (response != null && response.isSuccessful()));

                if (response != null && response.isSuccessful() && response.body() != null && response.body().getResult() != null) {
                    handleAuthenticatedResponse(response.body().getResult(), callback);
                } else {
                    ErrorInfo errorInfo = extractErrorInfo(response);
                    android.util.Log.d("AuthRepository", "Login error extracted - Code: " + errorInfo.getCode() + ", Message: " + errorInfo.getMessage());
                    callback.onResult(AuthResult.error(errorInfo.getMessage(), errorInfo.getCode()));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<TokenResponse>> call, Throwable t) {
                android.util.Log.e("AuthRepository", "Login failed with exception: " + t.getMessage());
                callback.onResult(AuthResult.error("Connection error: " + t.getMessage()));
            }
        });
    }

    public void registerWithEmail(String email, String password, String username, RepositoryCallback<String> callback) {
        callback.onResult(AuthResult.loading());
        RegisterRequest request = new RegisterRequest(username, username, email, password);

        apiService.registerWithEmail(request).enqueue(new Callback<ApiResponse<String>>() {
            @Override
            public void onResponse(Call<ApiResponse<String>> call,
                                   Response<ApiResponse<String>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onResult(AuthResult.success(response.body().getResult()));
                } else {
                    callback.onResult(AuthResult.error("Registration failed"));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<String>> call, Throwable t) {
                callback.onResult(AuthResult.error("Connection error: " + t.getMessage()));
            }
        });
    }

    public void verifyEmailOtp(String email, String otpCode, RepositoryCallback<UserResponse> callback) {
        callback.onResult(AuthResult.loading());
        EmailVerifyOtpRequest request = new EmailVerifyOtpRequest(email, otpCode);

        apiService.verifyEmailOtp(request).enqueue(new Callback<ApiResponse<TokenResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<TokenResponse>> call,
                                   Response<ApiResponse<TokenResponse>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getResult() != null) {
                    handleAuthenticatedResponse(response.body().getResult(), callback);
                } else {
                    callback.onResult(AuthResult.error("Invalid OTP"));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<TokenResponse>> call, Throwable t) {
                callback.onResult(AuthResult.error("Connection error: " + t.getMessage()));
            }
        });
    }

    public void loginWithGoogle(String idToken, RepositoryCallback<UserResponse> callback) {
        callback.onResult(AuthResult.loading());
        GoogleLoginRequest request = new GoogleLoginRequest(idToken);

        apiService.loginWithGoogle(request).enqueue(new Callback<ApiResponse<TokenResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<TokenResponse>> call,
                                   Response<ApiResponse<TokenResponse>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getResult() != null) {
                    handleAuthenticatedResponse(response.body().getResult(), callback);
                } else {
                    callback.onResult(AuthResult.error("Google login failed"));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<TokenResponse>> call, Throwable t) {
                callback.onResult(AuthResult.error("Connection error: " + t.getMessage()));
            }
        });
    }

    public void sendPasswordResetEmail(String email, RepositoryCallback<String> callback) {
        callback.onResult(AuthResult.loading());
        ForgotPasswordRequest request = new ForgotPasswordRequest(email);

        apiService.forgotPassword(request).enqueue(new Callback<ApiResponse<String>>() {
            @Override
            public void onResponse(Call<ApiResponse<String>> call,
                                   Response<ApiResponse<String>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onResult(AuthResult.success(response.body().getResult()));
                } else {
                    callback.onResult(AuthResult.error("Failed to send reset email"));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<String>> call, Throwable t) {
                callback.onResult(AuthResult.error("Connection error: " + t.getMessage()));
            }
        });
    }

    public void resetPassword(String email, String otpCode, String newPassword, RepositoryCallback<String> callback) {
        callback.onResult(AuthResult.loading());
        ResetPasswordRequest request = new ResetPasswordRequest(email, otpCode, newPassword);

        apiService.resetPassword(request).enqueue(new Callback<ApiResponse<String>>() {
            @Override
            public void onResponse(Call<ApiResponse<String>> call,
                                   Response<ApiResponse<String>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onResult(AuthResult.success(response.body().getResult()));
                } else {
                    callback.onResult(AuthResult.error("Failed to reset password"));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<String>> call, Throwable t) {
                callback.onResult(AuthResult.error("Connection error: " + t.getMessage()));
            }
        });
    }

    public void sendPhoneOtp(String phoneNumber, RepositoryCallback<String> callback) {
        callback.onResult(AuthResult.loading());
        PhoneSendOtpRequest request = new PhoneSendOtpRequest(phoneNumber);

        apiService.sendPhoneOtp(request).enqueue(new Callback<ApiResponse<String>>() {
            @Override
            public void onResponse(Call<ApiResponse<String>> call,
                                   Response<ApiResponse<String>> response) {
                if (response.isSuccessful()) {
                    callback.onResult(AuthResult.success("OTP Sent"));
                } else {
                    callback.onResult(AuthResult.error("Failed to send OTP"));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<String>> call, Throwable t) {
                callback.onResult(AuthResult.error("Connection error: " + t.getMessage()));
            }
        });
    }

    public void verifyOtp(String phone, String code, RepositoryCallback<UserResponse> callback) {
        callback.onResult(AuthResult.loading());
        PhoneVerifyOtpRequest request = new PhoneVerifyOtpRequest(phone, code);

        apiService.verifyPhoneOtp(request).enqueue(new Callback<ApiResponse<TokenResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<TokenResponse>> call,
                                   Response<ApiResponse<TokenResponse>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getResult() != null) {
                    handleAuthenticatedResponse(response.body().getResult(), callback);
                } else {
                    callback.onResult(AuthResult.error("Invalid OTP"));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<TokenResponse>> call, Throwable t) {
                callback.onResult(AuthResult.error("Connection error: " + t.getMessage()));
            }
        });
    }

    public void logout() {
        String refreshToken = tokenManager.getRefreshToken();
        if (refreshToken != null) {
            apiService.logout(new RefreshTokenRequest(refreshToken)).enqueue(new Callback<ApiResponse<String>>() {
                @Override
                public void onResponse(Call<ApiResponse<String>> call,
                                       Response<ApiResponse<String>> response) {
                }

                @Override
                public void onFailure(Call<ApiResponse<String>> call, Throwable t) {
                }
            });
        }
        tokenManager.clear();
    }

    public void sendEmailOtp(String email, RepositoryCallback<String> callback) {
        callback.onResult(AuthResult.loading());
        SendEmailOtpRequest request = new SendEmailOtpRequest(email);

        android.util.Log.d("AuthRepository", "Sending email OTP to: " + email);
        apiService.sendEmailOtp(request).enqueue(new Callback<ApiResponse<String>>() {
            @Override
            public void onResponse(Call<ApiResponse<String>> call,
                                   Response<ApiResponse<String>> response) {
                android.util.Log.d("AuthRepository", "Send email OTP response - Code: " + (response != null ? response.code() : "null"));
                if (response != null && response.isSuccessful() && response.body() != null) {
                    android.util.Log.d("AuthRepository", "Email OTP sent successfully");
                    callback.onResult(AuthResult.success("OTP sent successfully"));
                } else {
                    String errorMsg = extractErrorMessage(response, "Failed to send OTP");
                    android.util.Log.e("AuthRepository", "Failed to send email OTP: " + errorMsg);
                    callback.onResult(AuthResult.error(errorMsg));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<String>> call, Throwable t) {
                android.util.Log.e("AuthRepository", "Send email OTP failed with exception: " + t.getMessage());
                callback.onResult(AuthResult.error("Connection error: " + t.getMessage()));
            }
        });
    }

    private void handleAuthenticatedResponse(TokenResponse tokenResponse,
                                             RepositoryCallback<UserResponse> callback) {
        tokenManager.saveTokens(tokenResponse.getAccessToken(), tokenResponse.getRefreshToken());
        tokenManager.saveUser(tokenResponse.getUser());
        ThemeSyncManager.syncThemeIfAuthenticated(context,
                () -> callback.onResult(AuthResult.success(tokenResponse.getUser())));
    }

    private <T> String extractErrorMessage(Response<T> response, String fallback) {
        if (response == null) {
            return fallback;
        }

        try {
            if (response.errorBody() != null) {
                String raw = response.errorBody().string();
                if (raw != null && !raw.trim().isEmpty()) {
                    Type type = new TypeToken<ApiResponse<Object>>() {
                    }.getType();
                    ApiResponse<Object> apiError = gson.fromJson(raw, type);
                    if (apiError != null && apiError.getMessage() != null && !apiError.getMessage().trim().isEmpty()) {
                        return apiError.getMessage();
                    }
                }
            }
        } catch (Exception ignored) {
            // Ignore parse errors and use the fallback.
        }

        return fallback;
    }

    private <T> int extractErrorCode(Response<T> response) {
        if (response == null) {
            return -1;
        }

        try {
            if (response.errorBody() != null) {
                String raw = response.errorBody().string();
                if (raw != null && !raw.trim().isEmpty()) {
                    Type type = new TypeToken<ApiResponse<Object>>() {
                    }.getType();
                    ApiResponse<Object> apiError = gson.fromJson(raw, type);
                    if (apiError != null) {
                        return apiError.getCode();
                    }
                }
            }
        } catch (Exception ignored) {
            // Ignore parse errors
        }

        return -1;
    }

    private <T> ErrorInfo extractErrorInfo(Response<T> response) {
        if (response == null) {
            return new ErrorInfo(-1, "Login failed");
        }

        try {
            if (response.errorBody() != null) {
                String raw = response.errorBody().string();
                android.util.Log.d("AuthRepository", "Error body: " + raw);

                if (raw != null && !raw.trim().isEmpty()) {
                    Type type = new TypeToken<ApiResponse<Object>>() {
                    }.getType();
                    ApiResponse<Object> apiError = gson.fromJson(raw, type);
                    if (apiError != null) {
                        String message = apiError.getMessage();
                        int code = apiError.getCode();
                        android.util.Log.d("AuthRepository", "Parsed error - Code: " + code + ", Message: " + message);
                        return new ErrorInfo(code, message != null && !message.trim().isEmpty() ? message : "Login failed");
                    }
                }
            } else {
                android.util.Log.d("AuthRepository", "Error body is null");
            }
        } catch (Exception e) {
            android.util.Log.e("AuthRepository", "Error parsing error response: " + e.getMessage());
        }

        return new ErrorInfo(-1, "Login failed");
    }

    // Helper class to hold error code and message
    private static class ErrorInfo {
        private final int code;
        private final String message;

        ErrorInfo(int code, String message) {
            this.code = code;
            this.message = message;
        }

        int getCode() {
            return code;
        }

        String getMessage() {
            return message;
        }
    }
}