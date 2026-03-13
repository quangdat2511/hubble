package com.hubble.service;

import com.hubble.configuration.properties.StorageProperties;
import com.hubble.dto.response.UploadResponse;
import com.hubble.entity.Attachment;
import com.hubble.exception.AppException;
import com.hubble.exception.ErrorCode;
import com.hubble.repository.AttachmentRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;


@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MediaService {
    StorageService storageService;
    StorageProperties storageProperties;
    AttachmentRepository attachmentRepository;

    public UploadResponse uploadMedia(MultipartFile file, String folder) {
        // 1. Validate file
        validateFile(file);

        // 2. Upload to storage
        String url;
        try {
            url = storageService.upload(file, folder);
        } catch (Exception e) {
            log.error("Upload failed", e);
            throw new AppException(ErrorCode.UPLOAD_FAILED);
        }

        // 3. Save a "pending" attachment (not yet linked to a message)
        Attachment attachment = Attachment.builder()
                .messageId(null) // will be linked when message is sent
                .filename(file.getOriginalFilename())
                .url(url)
                .contentType(file.getContentType())
                .sizeBytes(file.getSize())
                .build();

        attachment = attachmentRepository.save(attachment);

        return UploadResponse.builder()
                .attachmentId(attachment.getId())
                .url(url)
                .filename(file.getOriginalFilename())
                .contentType(file.getContentType())
                .sizeBytes(file.getSize())
                .build();
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new AppException(ErrorCode.FILE_EMPTY);
        }

        long maxBytes = (long) storageProperties.getMaxFileSizeMb() * 1024 * 1024;
        if (file.getSize() > maxBytes) {
            throw new AppException(ErrorCode.FILE_TOO_LARGE);
        }

        if (!storageProperties.getAllowedTypes().contains(file.getContentType())) {
            throw new AppException(ErrorCode.FILE_TYPE_NOT_ALLOWED);
        }
    }
}
