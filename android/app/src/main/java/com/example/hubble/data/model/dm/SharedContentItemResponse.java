package com.example.hubble.data.model.dm;

import android.text.TextUtils;

import com.google.gson.annotations.SerializedName;

public class SharedContentItemResponse {

    @SerializedName("id")
    private String id;

    @SerializedName("messageId")
    private String messageId;

    @SerializedName("type")
    private String type;

    @SerializedName("url")
    private String url;

    @SerializedName("previewUrl")
    private String previewUrl;

    @SerializedName("filename")
    private String filename;

    @SerializedName("contentType")
    private String contentType;

    @SerializedName("sizeBytes")
    private long sizeBytes;

    @SerializedName("messageContent")
    private String messageContent;

    @SerializedName("createdAt")
    private String createdAt;

    public String getId() {
        return id;
    }

    public String getMessageId() {
        return messageId;
    }

    public String getType() {
        return type;
    }

    public String getUrl() {
        return url;
    }

    public String getPreviewUrl() {
        return TextUtils.isEmpty(previewUrl) ? url : previewUrl;
    }

    public String getFilename() {
        return filename;
    }

    public String getContentType() {
        return contentType;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public String getMessageContent() {
        return messageContent;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public boolean isMedia() {
        return "MEDIA".equalsIgnoreCase(type);
    }

    public boolean isLink() {
        return "LINK".equalsIgnoreCase(type);
    }

    public boolean isFile() {
        return "FILE".equalsIgnoreCase(type);
    }

    public boolean isVideo() {
        return !TextUtils.isEmpty(contentType) && contentType.toLowerCase().startsWith("video/");
    }

    public String getResolvedFileName() {
        if (!TextUtils.isEmpty(filename)) {
            return filename;
        }
        if (!TextUtils.isEmpty(url)) {
            int slash = url.lastIndexOf('/');
            if (slash >= 0 && slash < url.length() - 1) {
                return url.substring(slash + 1);
            }
        }
        if (isLink()) {
            return "shared-link";
        }
        return "shared-item";
    }
}
