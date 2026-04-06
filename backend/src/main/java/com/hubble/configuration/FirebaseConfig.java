package com.hubble.configuration;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Configuration
@Slf4j
public class FirebaseConfig {

    @Value("${firebase.config.path}")
    private String firebaseConfigPath;

    @Bean
    public FirebaseApp firebaseApp() throws Exception {
        // Kiểm tra xem Firebase đã được khởi tạo chưa để tránh lỗi duplicate
        if (!FirebaseApp.getApps().isEmpty()) {
            log.info("Firebase app already initialized");
            return FirebaseApp.getInstance();
        }

        try {
            FirebaseOptions options;

            // 1. Thử đọc cấu hình từ biến môi trường (Áp dụng khi chạy trên Railway)
            String firebaseEnv = System.getenv("FIREBASE_CREDENTIALS");

            if (firebaseEnv != null && !firebaseEnv.trim().isEmpty()) {
                log.info("Initializing Firebase using Environment Variable (Cloud Mode - Railway)");
                InputStream credentialsStream = new ByteArrayInputStream(firebaseEnv.getBytes(StandardCharsets.UTF_8));
                options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(credentialsStream))
                        .build();
            } else {
                // 2. Nếu không có biến môi trường, chuyển sang đọc file vật lý (Áp dụng khi chạy Local)
                log.info("Initializing Firebase using physical file path: {} (Local Mode)", firebaseConfigPath);
                InputStream serviceAccount = new ClassPathResource(firebaseConfigPath).getInputStream();
                options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                        .build();
            }

            FirebaseApp app = FirebaseApp.initializeApp(options);
            log.info("Firebase initialized successfully");
            return app;

        } catch (Exception e) {
            log.error("Failed to initialize Firebase", e);
            throw e;
        }
    }
}
