package com.example.hubble.data.model.me;

public class AvatarResponse {
    private String avatarUrl;
    private String fileName;
    private String contentType;

    public AvatarResponse() {
    }

    public AvatarResponse(String avatarUrl, String fileName, String contentType) {
        this.avatarUrl = avatarUrl;
        this.fileName = fileName;
        this.contentType = contentType;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }
}