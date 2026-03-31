package com.hubble.service;

import org.springframework.web.multipart.MultipartFile;

public interface StorageService {
    String upload(MultipartFile file, String folder) throws Exception;
    void delete(String objectKey) throws Exception;
}
