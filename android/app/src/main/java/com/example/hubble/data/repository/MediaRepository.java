package com.example.hubble.data.repository;



import android.content.Context;
import android.net.Uri;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.hubble.data.api.ApiService;
import com.example.hubble.data.api.RetrofitClient;
import com.example.hubble.data.model.ApiResponse;
import com.example.hubble.data.model.UploadResponse;
import com.example.hubble.utils.FileUtils;

import java.io.File;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MediaRepository {

    private final ApiService apiService;
    private final Context context;

    public MediaRepository(Context context) {
        this.context = context.getApplicationContext();
        this.apiService = RetrofitClient.getApiService();  // ← your actual client
    }

    public LiveData<UploadResult> uploadMedia(Uri fileUri, String folder) {
        MutableLiveData<UploadResult> result = new MutableLiveData<>();

        // Post loading state immediately
        result.postValue(UploadResult.loading());

        // Resolve URI to a real File
        File file = FileUtils.getFileFromUri(context, fileUri);
        if (file == null) {
            result.postValue(UploadResult.error("Could not read file"));
            return result;
        }

        String originalFileName = fileUri.getLastPathSegment();
        if (originalFileName == null) {
            originalFileName = file.getName();
        }

        String mimeType = context.getContentResolver().getType(fileUri);

        if (mimeType == null || mimeType.equals("application/octet-stream")) {
            String lowerCaseName = originalFileName.toLowerCase();
            if (lowerCaseName.endsWith(".m4a") || lowerCaseName.endsWith(".mp4")) {
                mimeType = "audio/mp4"; // Ép chuẩn audio
            } else if (lowerCaseName.endsWith(".jpg") || lowerCaseName.endsWith(".jpeg")) {
                mimeType = "image/jpeg";
            } else if (lowerCaseName.endsWith(".png")) {
                mimeType = "image/png";
            } else if (lowerCaseName.endsWith(".pdf")) {
                mimeType = "application/pdf";
            } else {
                mimeType = "application/octet-stream";
            }
        }

        RequestBody requestBody = RequestBody.create(MediaType.parse(mimeType), file);

        MultipartBody.Part filePart = MultipartBody.Part.createFormData(
                "file",
                originalFileName,
                requestBody
        );

        apiService.uploadMedia(filePart, folder).enqueue(new Callback<ApiResponse<UploadResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<UploadResponse>> call,
                                   Response<ApiResponse<UploadResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    result.postValue(UploadResult.success(response.body().getResult()));
                } else {
                    String serverMessage = "";
                    try {
                        if (response.errorBody() != null) {
                            serverMessage = response.errorBody().string(); // Đọc lời nhắn từ Spring Boot
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    android.util.Log.e("VOICE_TEST", "Lỗi từ Server: " + serverMessage);
                    result.postValue(UploadResult.error("Upload failed: " + response.code()));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<UploadResponse>> call, Throwable t) {
                result.postValue(UploadResult.error("Network error: " + t.getMessage()));
            }
        });

        return result;
    }

    // --- Result wrapper ---
    public static class UploadResult {
        public enum Status { LOADING, SUCCESS, ERROR }

        public final Status status;
        public final UploadResponse data;
        public final String errorMessage;

        private UploadResult(Status status, UploadResponse data, String errorMessage) {
            this.status = status;
            this.data = data;
            this.errorMessage = errorMessage;
        }

        public static UploadResult loading() {
            return new UploadResult(Status.LOADING, null, null);
        }

        public static UploadResult success(UploadResponse data) {
            return new UploadResult(Status.SUCCESS, data, null);
        }

        public static UploadResult error(String message) {
            return new UploadResult(Status.ERROR, null, message);
        }
    }
}