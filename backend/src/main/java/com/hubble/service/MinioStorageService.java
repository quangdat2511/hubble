package com.hubble.service;

import com.hubble.configuration.properties.StorageProperties;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.storage.provider", havingValue = "minio")
public class MinioStorageService implements  StorageService {

    private final MinioClient minioClient;
    private final StorageProperties props;
    @Override
    public String upload(MultipartFile file, String folder) throws Exception {
        String extension = getExtension(file.getOriginalFilename());
        String objectKey = folder + "/" + UUID.randomUUID().toString() + "." + extension;

        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(props.getBucket())
                        .object(objectKey)
                        .stream(file.getInputStream(), file.getSize(), -1)
                        .contentType(file.getContentType())
                        .build()
        );
        return props.getPublicUrl() + "/" + objectKey;
    }
    @Override
    public void delete(String objectKey) throws Exception {
        minioClient.removeObject(
                RemoveObjectArgs.builder()
                        .bucket(props.getBucket())
                        .object(objectKey)
                        .build()
        );
    }
    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "bin";
        return filename.substring(filename.lastIndexOf(".") + 1);
    }
}
