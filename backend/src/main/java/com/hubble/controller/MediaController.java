package com.hubble.controller;

import com.hubble.dto.common.ApiResponse;
import com.hubble.dto.response.UploadResponse;
import com.hubble.service.MediaService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/media")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MediaController {

    private final MediaService mediaService;


    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<UploadResponse>> upload(
            @RequestPart("file") MultipartFile file,
            @RequestParam(defaultValue = "media") String folder
    ) {
        UploadResponse response = mediaService.uploadMedia(file, folder);
        return ResponseEntity.ok(ApiResponse.<UploadResponse>builder()
                .result(response)
                .build());
    }
}