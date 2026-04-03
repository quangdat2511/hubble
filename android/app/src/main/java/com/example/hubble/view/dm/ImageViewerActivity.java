package com.example.hubble.view.dm;

import android.app.DownloadManager;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.hubble.R;
import com.example.hubble.utils.TokenManager;

public class ImageViewerActivity extends AppCompatActivity {

    public static final String EXTRA_IMAGE_URL = "extra_image_url";
    public static final String EXTRA_FILE_NAME = "extra_file_name";

    private String imageUrl;
    private String fileName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_viewer);

        // Nhận dữ liệu từ màn hình Chat truyền sang
        imageUrl = getIntent().getStringExtra(EXTRA_IMAGE_URL);
        fileName = getIntent().getStringExtra(EXTRA_FILE_NAME);

        if (fileName == null || fileName.isEmpty()) {
            fileName = "image_" + System.currentTimeMillis() + ".jpg";
        }

        ImageView ivPhoto = findViewById(R.id.ivPhoto);
        ImageView ivClose = findViewById(R.id.ivClose);
        ImageView ivMenu = findViewById(R.id.ivMenu);

        // Load ảnh lên màn hình bằng Glide
        Glide.with(this).load(imageUrl).into(ivPhoto);

        // Nút X để đóng
        ivClose.setOnClickListener(v -> finish());

        // Nút 3 chấm mở Menu
        ivMenu.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(this, ivMenu);
            popup.getMenu().add(0, 1, 0, "Lưu hình ảnh"); // Tạo option Tải ảnh

            popup.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == 1) {
                    downloadImage();
                    return true;
                }
                return false;
            });
            popup.show();
        });
    }

    // Hàm tải ảnh về máy
    private void downloadImage() {
        if (imageUrl == null || imageUrl.isEmpty()) return;

        String finalUrl = imageUrl;
        if (finalUrl.contains("localhost")) {
            finalUrl = finalUrl.replace("localhost", "10.0.2.2");
        }

        String safeFileName = fileName;
        if (safeFileName.contains("/")) safeFileName = safeFileName.substring(safeFileName.lastIndexOf("/") + 1);
        if (safeFileName.contains(":")) safeFileName = safeFileName.substring(safeFileName.lastIndexOf(":") + 1);
        safeFileName = safeFileName.replaceAll("[\\\\/:*?\"<>|]", "_");
        if (!safeFileName.contains(".")) {
            safeFileName = safeFileName + ".jpg";
        }
        try {
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(finalUrl));
            request.setTitle(safeFileName);
            request.setDescription("Đang tải hình ảnh...");
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, safeFileName);

            TokenManager tokenManager = new TokenManager(this);
            if (tokenManager.getAccessToken() != null) {
                request.addRequestHeader("Authorization", "Bearer " + tokenManager.getAccessToken());
            }

            DownloadManager downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
            if (downloadManager != null) {
                downloadManager.enqueue(request);
                Toast.makeText(this, "Đang tải ảnh về máy...", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Lỗi khi tải ảnh: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}