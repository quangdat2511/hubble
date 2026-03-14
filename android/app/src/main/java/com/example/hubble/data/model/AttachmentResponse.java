package com.example.hubble.data.model;

import com.google.gson.annotations.SerializedName;

public class AttachmentResponse {

    @SerializedName("id")
    private String id;

    @SerializedName("url")
    private String url;

    @SerializedName("filename")
    private String filename;

    @SerializedName("contentType")
    private String contentType;

    @SerializedName("sizeBytes")
    private long sizeBytes;

    public String getId() { return id; }
    public String getUrl() { return url; }
    public String getFilename() { return filename; }
    public String getContentType() { return contentType; }
    public long getSizeBytes() { return sizeBytes; }
}
