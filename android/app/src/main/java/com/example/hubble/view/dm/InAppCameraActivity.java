package com.example.hubble.view.dm;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Chronometer;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.video.FileOutputOptions;
import androidx.camera.video.Quality;
import androidx.camera.video.QualitySelector;
import androidx.camera.video.Recorder;
import androidx.camera.video.Recording;
import androidx.camera.video.VideoCapture;
import androidx.camera.video.VideoRecordEvent;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.example.hubble.R;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.util.concurrent.ExecutionException;

public class InAppCameraActivity extends AppCompatActivity {

    private PreviewView viewFinder;
    private ImageButton btnCapture;
    private Chronometer tvTimer;

    private ImageCapture imageCapture;
    private VideoCapture<Recorder> videoCapture;
    private Recording recording;

    // Các biến để xử lý Chạm / Nhấn giữ
    private Handler touchHandler = new Handler();
    private boolean isLongPress = false;
    private Runnable longPressRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_in_app_camera);

        viewFinder = findViewById(R.id.viewFinder);
        btnCapture = findViewById(R.id.btnCapture);
        tvTimer = findViewById(R.id.tvTimer);

        // Yêu cầu bạn đã xin quyền CAMERA và RECORD_AUDIO trước khi gọi hàm này
        startCamera();
        setupCaptureButton();
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                // 1. Ống ngắm (Preview)
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(viewFinder.getSurfaceProvider());

                // 2. Trình chụp ảnh (Image)
                imageCapture = new ImageCapture.Builder().build();

                // 3. Trình quay video (Video)
                Recorder recorder = new Recorder.Builder()
                        .setQualitySelector(QualitySelector.from(Quality.HD)) // Chất lượng HD cho nhẹ
                        .build();
                videoCapture = VideoCapture.withOutput(recorder);

                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

                cameraProvider.unbindAll();
                // Buộc cả 3 UseCase vào Camera
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture, videoCapture);

            } catch (ExecutionException | InterruptedException e) {
                Log.e("CameraX", "Lỗi khởi tạo camera", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    // --- KỸ THUẬT: PHÂN BIỆT CHẠM VÀ NHẤN GIỮ ---
    @SuppressLint("ClickableViewAccessibility")
    private void setupCaptureButton() {
        btnCapture.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    isLongPress = false;
                    // Bấm xuống thì hẹn giờ, nếu quá 300ms mà chưa buông tay thì là quay video
                    longPressRunnable = () -> {
                        isLongPress = true;
                        startVideoRecording();
                    };
                    touchHandler.postDelayed(longPressRunnable, 300);
                    break;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    touchHandler.removeCallbacks(longPressRunnable); // Hủy hẹn giờ

                    if (isLongPress) {
                        // Đang quay mà buông tay ra -> Dừng quay
                        stopVideoRecording();
                    } else {
                        // Buông tay ra ngay lập tức (dưới 300ms) -> Chụp ảnh
                        takePhoto();
                    }
                    break;
            }
            return true;
        });
    }

    private void takePhoto() {
        if (imageCapture == null) return;

        // Báo hiệu bằng cách nháy nút hoặc hiệu ứng âm thanh (tùy chọn)

        File photoFile = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "Hubble_IMG_" + System.currentTimeMillis() + ".jpg");
        ImageCapture.OutputFileOptions outputOptions = new ImageCapture.OutputFileOptions.Builder(photoFile).build();

        imageCapture.takePicture(outputOptions, ContextCompat.getMainExecutor(this), new ImageCapture.OnImageSavedCallback() {
            @Override
            public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                returnResult(photoFile.getAbsolutePath(), "IMAGE");
            }
            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                Toast.makeText(InAppCameraActivity.this, "Lỗi chụp ảnh", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @SuppressLint("MissingPermission") // Cần check quyền Audio trước
    private void startVideoRecording() {
        if (videoCapture == null) return;

        File videoFile = new File(getExternalFilesDir(Environment.DIRECTORY_MOVIES), "Hubble_VID_" + System.currentTimeMillis() + ".mp4");
        FileOutputOptions fileOutputOptions = new FileOutputOptions.Builder(videoFile).build();

        // Đổi màu nút để báo đang quay và hiện đồng hồ
        btnCapture.setBackgroundTintList(ColorStateList.valueOf(Color.RED));
        tvTimer.setVisibility(View.VISIBLE);
        tvTimer.setBase(SystemClock.elapsedRealtime());
        tvTimer.start();

        recording = videoCapture.getOutput()
                .prepareRecording(this, fileOutputOptions)
                .withAudioEnabled() // Bật ghi âm
                .start(ContextCompat.getMainExecutor(this), recordEvent -> {
                    if (recordEvent instanceof VideoRecordEvent.Finalize) {
                        VideoRecordEvent.Finalize finalizeEvent = (VideoRecordEvent.Finalize) recordEvent;
                        if (!finalizeEvent.hasError()) {
                            returnResult(videoFile.getAbsolutePath(), "VIDEO");
                        } else {
                            recording.close();
                            recording = null;
                            Toast.makeText(this, "Lỗi lưu video", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void stopVideoRecording() {
        if (recording != null) {
            recording.stop();
            recording = null;
        }
        // Trả UI về bình thường
        btnCapture.setBackgroundTintList(null);
        tvTimer.stop();
        tvTimer.setVisibility(View.GONE);
    }

    // --- HÀM TRẢ FILE VỀ CHO MÀN HÌNH CHAT ---
    private void returnResult(String filePath, String mediaType) {
        Intent intent = new Intent();
        intent.putExtra("MEDIA_PATH", filePath);
        intent.putExtra("MEDIA_TYPE", mediaType); // "IMAGE" hoặc "VIDEO"
        setResult(RESULT_OK, intent);
        finish(); // Đóng camera, trở về màn hình Chat
    }
}