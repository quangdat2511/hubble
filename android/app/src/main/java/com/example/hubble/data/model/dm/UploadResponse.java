package com.example.hubble.data.model.dm;

import com.google.gson.annotations.SerializedName;

public class UploadResponse {

    @SerializedName("attachmentId")
    private String attachmentId;

    @SerializedName("url")
    private String url;

    @SerializedName("filename")
    private String filename;

    @SerializedName("contentType")
    private String contentType;

    @SerializedName("sizeBytes")
    private long sizeBytes;

    public String getAttachmentId() { return attachmentId; }
    public String getUrl() { return url; }
    public String getFilename() { return filename; }
    public String getContentType() { return contentType; }
    public long getSizeBytes() { return sizeBytes; }
}
