package com.hubble.service;

import com.hubble.configuration.properties.StorageProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.storage.provider", havingValue = "supabase")
public class SupabaseStorageService implements StorageService {

    private final StorageProperties props;
    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public String upload(MultipartFile file, String folder) throws Exception {
        String extension = getExtension(file.getOriginalFilename());
        String objectKey = folder + "/" + UUID.randomUUID().toString() + "." + extension;

        // API Endpoint của Supabase: https://[project-ref].supabase.co/storage/v1/object/[bucket]/[path]
        String uploadUrl = props.getEndpoint() + "/storage/v1/object/" + props.getBucket() + "/" + objectKey;

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(props.getSecretKey()); // Dùng service_role_key của Supabase
        headers.setContentType(MediaType.valueOf(file.getContentType()));

        HttpEntity<byte[]> requestEntity = new HttpEntity<>(file.getBytes(), headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    uploadUrl,
                    HttpMethod.POST,
                    requestEntity,
                    String.class
            );

            if (!response.getStatusCode().is2xxSuccessful()) {
                log.error("Supabase upload failed. Status: {}, Body: {}", response.getStatusCode(), response.getBody());
                throw new Exception("Upload to Supabase failed");
            }

            // Trả về public URL
            return props.getPublicUrl() + "/" + objectKey;

        } catch (Exception e) {
            log.error("Exception during Supabase upload", e);
            throw e;
        }
    }

    @Override
    public void delete(String objectKey) throws Exception {
        String deleteUrl = props.getEndpoint() + "/storage/v1/object/" + props.getBucket() + "/" + objectKey;

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(props.getSecretKey());

        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
                deleteUrl,
                HttpMethod.DELETE,
                requestEntity,
                String.class
        );

        if (!response.getStatusCode().is2xxSuccessful()) {
            log.error("Supabase delete failed. Status: {}, Body: {}", response.getStatusCode(), response.getBody());
            throw new Exception("Delete from Supabase failed");
        }
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "bin";
        return filename.substring(filename.lastIndexOf(".") + 1);
    }
}