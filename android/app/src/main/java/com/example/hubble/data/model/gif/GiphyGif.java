package com.example.hubble.data.model.gif;

import com.google.gson.annotations.SerializedName;

public class GiphyGif {
    @SerializedName("id")
    public String id;

    @SerializedName("title")
    public String title;

    @SerializedName("type")
    public String type;

    @SerializedName("images")
    public GiphyImages images;

    /** URL to display in the picker (smaller, faster to load) */
    public String getPreviewUrl() {
        if (images == null) return null;
        if (images.fixedHeight != null && images.fixedHeight.url != null) {
            return images.fixedHeight.url;
        }
        if (images.previewGif != null && images.previewGif.url != null) {
            return images.previewGif.url;
        }
        if (images.downsized != null && images.downsized.url != null) {
            return images.downsized.url;
        }
        return getOriginalUrl();
    }

    /** URL to send in the message (higher quality) */
    public String getOriginalUrl() {
        if (images != null && images.original != null) {
            return images.original.url;
        }
        return null;
    }

    /** Aspect ratio for StaggeredGrid height calculation */
    public float getAspectRatio() {
        if (images == null) return 1f;
        GiphyImage img = images.fixedHeight != null ? images.fixedHeight : images.original;
        if (img == null) return 1f;
        int w = img.getWidthInt();
        int h = img.getHeightInt();
        if (w <= 0 || h <= 0) return 1f;
        return (float) w / h;
    }
}
