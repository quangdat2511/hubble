package com.hubble.service;

import com.hubble.configuration.properties.StorageProperties;
import com.hubble.repository.AttachmentRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MediaService {
    StorageService storageService;
    StorageProperties storageProperties;
    AttachmentRepository attachmentRepository;
}
