package com.hubble.service;

import com.hubble.exception.AppException;
import com.hubble.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@Slf4j
public class AvatarStorageService {

    public static final String LEGACY_AVATAR_URL_PREFIX = "/uploads/avatars/";
    public static final Path LEGACY_AVATAR_FOLDER = Paths.get(System.getProperty("user.dir"), "uploads", "avatars")
            .toAbsolutePath()
            .normalize();

    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();
    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );
    private static final long MAX_SIZE_BYTES = 5L * 1024 * 1024;

    private final String supabaseUrl;
    private final String serviceRoleKey;
    private final String avatarBucket;
    private volatile boolean bucketEnsured;

    public AvatarStorageService(
            @Value("${supabase.url}") String supabaseUrl,
            @Value("${supabase.service-role-key}") String serviceRoleKey,
            @Value("${supabase.avatar-bucket}") String avatarBucket) {
        this.supabaseUrl = trimTrailingSlash(supabaseUrl);
        this.serviceRoleKey = serviceRoleKey;
        this.avatarBucket = avatarBucket;
    }

    public void validateAvatarFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_FILE);
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            throw new AppException(ErrorCode.UNSUPPORTED_FILE_TYPE);
        }
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new AppException(ErrorCode.FILE_TOO_LARGE);
        }
    }

    public String uploadAvatar(UUID userId, MultipartFile file) {
        validateAvatarFile(file);

        try {
            ensureBucketExists();
            String objectKey = buildObjectKey(userId, file.getOriginalFilename(), file.getContentType());
            uploadBytes(objectKey, file.getContentType(), file.getBytes());
            return buildPublicUrl(objectKey);
        } catch (IOException e) {
            log.error("Failed to read avatar file before upload", e);
            throw new AppException(ErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    public String migrateLegacyAvatar(UUID userId, Path legacyPath) {
        if (legacyPath == null || !Files.exists(legacyPath) || !Files.isRegularFile(legacyPath)) {
            throw new AppException(ErrorCode.AVATAR_NOT_FOUND);
        }

        try {
            ensureBucketExists();
            String contentType = detectContentType(legacyPath.getFileName().toString());
            String objectKey = buildObjectKey(userId, legacyPath.getFileName().toString(), contentType);
            uploadBytes(objectKey, contentType, Files.readAllBytes(legacyPath));
            return buildPublicUrl(objectKey);
        } catch (IOException e) {
            log.error("Failed to migrate legacy avatar {}", legacyPath, e);
            throw new AppException(ErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    public void deleteAvatarByUrl(String avatarUrl) {
        String objectKey = extractObjectKey(avatarUrl);
        if (objectKey == null) {
            return;
        }

        try {
            HttpRequest request = baseRequest("/storage/v1/object/" + avatarBucket + "/" + encodePath(objectKey))
                    .DELETE()
                    .build();
            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400 && response.statusCode() != 404) {
                log.warn("Failed to delete Supabase avatar [{}]: {}", response.statusCode(), response.body());
            }
        } catch (Exception e) {
            log.warn("Could not delete Supabase avatar {}", avatarUrl, e);
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public boolean isLegacyAvatarUrl(String avatarUrl) {
        return avatarUrl != null && avatarUrl.startsWith(LEGACY_AVATAR_URL_PREFIX);
    }

    public Path resolveLegacyPath(String avatarUrl) {
        if (!isLegacyAvatarUrl(avatarUrl)) {
            return null;
        }

        String fileName = Paths.get(avatarUrl).getFileName().toString();
        Path legacyPath = LEGACY_AVATAR_FOLDER.resolve(fileName).normalize();
        if (!legacyPath.startsWith(LEGACY_AVATAR_FOLDER)) {
            throw new AppException(ErrorCode.INVALID_FILE);
        }
        return legacyPath;
    }

    public void deleteLegacyAvatarQuietly(String avatarUrl) {
        Path legacyPath = resolveLegacyPath(avatarUrl);
        if (legacyPath == null) {
            return;
        }

        try {
            Files.deleteIfExists(legacyPath);
        } catch (IOException e) {
            log.warn("Could not delete legacy avatar {}", legacyPath, e);
        }
    }

    public String extractFileName(String avatarUrl) {
        if (!StringUtils.hasText(avatarUrl)) {
            throw new AppException(ErrorCode.AVATAR_NOT_FOUND);
        }
        return Paths.get(URI.create(avatarUrl).getPath()).getFileName().toString();
    }

    public String detectContentType(String fileName) {
        String extension = StringUtils.getFilenameExtension(fileName);
        if (extension == null) {
            return "application/octet-stream";
        }

        return switch (extension.toLowerCase(Locale.ROOT)) {
            case "png" -> "image/png";
            case "webp" -> "image/webp";
            default -> "image/jpeg";
        };
    }

    private synchronized void ensureBucketExists() {
        if (bucketEnsured) {
            return;
        }

        try {
            String body = "{\"id\":\"" + escapeJson(avatarBucket) + "\",\"name\":\"" + escapeJson(avatarBucket)
                    + "\",\"public\":true}";
            HttpRequest request = baseRequest("/storage/v1/bucket")
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            int statusCode = response.statusCode();
            if (statusCode >= 200 && statusCode < 300) {
                bucketEnsured = true;
                return;
            }

            String responseBody = response.body() == null ? "" : response.body().toLowerCase(Locale.ROOT);
            if (statusCode == 409 || responseBody.contains("already exists") || responseBody.contains("duplicate")) {
                bucketEnsured = true;
                return;
            }

            log.error("Failed to ensure avatar bucket [{}]: {}", statusCode, response.body());
            throw new AppException(ErrorCode.FILE_UPLOAD_FAILED);
        } catch (IOException e) {
            log.error("Failed to ensure avatar bucket", e);
            throw new AppException(ErrorCode.FILE_UPLOAD_FAILED);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Interrupted while ensuring avatar bucket", e);
            throw new AppException(ErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    private void uploadBytes(String objectKey, String contentType, byte[] bytes) {
        try {
            HttpRequest request = baseRequest("/storage/v1/object/" + avatarBucket + "/" + encodePath(objectKey))
                    .header("Content-Type", contentType)
                    .header("x-upsert", "true")
                    .POST(HttpRequest.BodyPublishers.ofByteArray(bytes))
                    .build();

            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.error("Supabase avatar upload failed [{}]: {}", response.statusCode(), response.body());
                throw new AppException(ErrorCode.FILE_UPLOAD_FAILED);
            }
        } catch (IOException e) {
            log.error("Failed to upload avatar to Supabase", e);
            throw new AppException(ErrorCode.FILE_UPLOAD_FAILED);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Interrupted while uploading avatar", e);
            throw new AppException(ErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    private HttpRequest.Builder baseRequest(String relativePath) {
        return HttpRequest.newBuilder()
                .uri(URI.create(supabaseUrl + relativePath))
                .header("Authorization", "Bearer " + serviceRoleKey)
                .header("apikey", serviceRoleKey);
    }

    private String buildObjectKey(UUID userId, String originalFilename, String contentType) {
        String safeFilename = sanitizeFilename(originalFilename, contentType);
        return "users/" + userId + "/" + Instant.now().toEpochMilli() + "-" + safeFilename;
    }

    private String sanitizeFilename(String originalFilename, String contentType) {
        String cleaned = StringUtils.cleanPath(originalFilename == null ? "" : originalFilename).trim();
        if (!StringUtils.hasText(cleaned)) {
            cleaned = "avatar" + defaultExtension(contentType);
        }

        String normalized = cleaned.replace("\\", "-")
                .replace("/", "-")
                .replaceAll("[^A-Za-z0-9._-]", "-")
                .replaceAll("-{2,}", "-");

        String extension = StringUtils.getFilenameExtension(normalized);
        if (!StringUtils.hasText(extension)) {
            normalized = normalized + defaultExtension(contentType);
        }
        return normalized.toLowerCase(Locale.ROOT);
    }

    private String defaultExtension(String contentType) {
        if ("image/png".equalsIgnoreCase(contentType)) {
            return ".png";
        }
        if ("image/webp".equalsIgnoreCase(contentType)) {
            return ".webp";
        }
        return ".jpg";
    }

    private String buildPublicUrl(String objectKey) {
        return supabaseUrl + "/storage/v1/object/public/" + avatarBucket + "/" + encodePath(objectKey);
    }

    private String extractObjectKey(String avatarUrl) {
        if (!StringUtils.hasText(avatarUrl)) {
            return null;
        }

        String prefix = supabaseUrl + "/storage/v1/object/public/" + avatarBucket + "/";
        if (!avatarUrl.startsWith(prefix)) {
            return null;
        }
        return avatarUrl.substring(prefix.length());
    }

    private String encodePath(String path) {
        String[] segments = path.split("/");
        StringBuilder encoded = new StringBuilder();
        for (int i = 0; i < segments.length; i++) {
            if (i > 0) {
                encoded.append('/');
            }
            encoded.append(URLEncoder.encode(segments[i], StandardCharsets.UTF_8));
        }
        return encoded.toString().replace("+", "%20");
    }

    private String trimTrailingSlash(String value) {
        if (value == null) {
            return null;
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
