package com.example.hubble.view.dm;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.PopupMenu;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.hubble.R;
import com.example.hubble.data.api.NetworkConfig;
import com.example.hubble.utils.InAppMessageUtils;
import com.example.hubble.utils.MediaDownloadHelper;
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

        imageUrl = getIntent().getStringExtra(EXTRA_IMAGE_URL);
        imageUrl = NetworkConfig.resolveUrl(imageUrl);
        fileName = getIntent().getStringExtra(EXTRA_FILE_NAME);

        if (fileName == null || fileName.isEmpty()) {
            fileName = "image_" + System.currentTimeMillis() + ".jpg";
        }

        ImageView ivPhoto = findViewById(R.id.ivPhoto);
        ImageView ivClose = findViewById(R.id.ivClose);
        ImageView ivMenu = findViewById(R.id.ivMenu);

        Glide.with(this).load(imageUrl).into(ivPhoto);

        ivClose.setOnClickListener(v -> finish());

        ivMenu.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(this, ivMenu);
            popup.getMenu().add(0, 1, 0, getString(R.string.dm_gallery_download));

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

    private void downloadImage() {
        if (imageUrl == null || imageUrl.isEmpty()) {
            return;
        }

        try {
            MediaDownloadHelper.EnqueueResult enqueueResult = MediaDownloadHelper.enqueueDownload(
                    this,
                    imageUrl,
                    fileName,
                    "image/*",
                    new TokenManager(this).getAccessToken(),
                    getString(R.string.dm_gallery_download_in_progress)
            );
            InAppMessageUtils.show(this, getString(R.string.dm_gallery_download_started, enqueueResult.getFileName()));
        } catch (Exception e) {
            String message = e.getMessage() != null ? e.getMessage() : getString(R.string.dm_gallery_download_failed);
            InAppMessageUtils.showLong(this, getString(R.string.dm_gallery_download_failed_with_reason, message));
        }
    }
}
