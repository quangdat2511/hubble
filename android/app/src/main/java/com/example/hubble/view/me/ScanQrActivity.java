package com.example.hubble.view.me;

import android.Manifest;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;

import com.example.hubble.R;
import com.example.hubble.databinding.ActivityScanQrBinding;
import com.google.android.material.snackbar.Snackbar;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;

public class ScanQrActivity extends androidx.appcompat.app.AppCompatActivity {

    private ActivityScanQrBinding binding;
    private boolean handledResult;

    private final ActivityResultLauncher<String> cameraPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    startScanner();
                } else {
                    Snackbar.make(binding.getRoot(), R.string.me_qr_camera_denied, Snackbar.LENGTH_LONG).show();
                }
            });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityScanQrBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.toolbar.setNavigationOnClickListener(v -> finish());
        binding.barcodeView.decodeContinuous(callback);
        cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!handledResult) {
            binding.barcodeView.resume();
        }
    }

    @Override
    protected void onPause() {
        binding.barcodeView.pause();
        super.onPause();
    }

    private void startScanner() {
        handledResult = false;
        binding.barcodeView.resume();
        binding.tvScannerHint.setText(R.string.me_qr_scan_hint);
    }

    private final BarcodeCallback callback = new BarcodeCallback() {
        @Override
        public void barcodeResult(BarcodeResult result) {
            if (handledResult || result == null || result.getText() == null) {
                return;
            }

            String qrToken = extractToken(result.getText());
            if (qrToken == null || qrToken.trim().isEmpty()) {
                binding.tvScannerHint.setText(R.string.me_qr_invalid);
                return;
            }

            handledResult = true;
            binding.barcodeView.pause();

            Intent intent = new Intent(ScanQrActivity.this, QrScanResultActivity.class);
            intent.putExtra(QrScanResultActivity.EXTRA_QR_TOKEN, qrToken);
            startActivity(intent);
            finish();
        }

        @Override
        public void possibleResultPoints(java.util.List<com.google.zxing.ResultPoint> resultPoints) {
        }
    };

    @Nullable
    private String extractToken(String rawValue) {
        if (rawValue == null || rawValue.trim().isEmpty()) {
            return null;
        }

        String trimmed = rawValue.trim();
        if (!trimmed.startsWith("hubble://")) {
            return trimmed;
        }

        Uri uri = Uri.parse(trimmed);
        return uri.getQueryParameter("token");
    }
}
