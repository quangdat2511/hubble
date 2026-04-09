package com.hubble.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "user_settings")
public class UserSettings {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "theme")
    private String theme;

    @Column(name = "locale")
    private String locale;

    @Column(name = "app_lock_pin")
    private String appLockPin;

    @Column(name = "notification_enabled")
    private Boolean notificationEnabled;

    @Column(name = "notification_sound")
    private Boolean notificationSound;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        normalizeTheme();
        if (theme == null) {
            theme = "SYSTEM";
        }
        if (locale == null) {
            locale = "vi";
        }
        if (notificationEnabled == null) {
            notificationEnabled = true;
        }
        if (notificationSound == null) {
            notificationSound = true;
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        normalizeTheme();
        updatedAt = LocalDateTime.now();
    }

    void normalizeTheme() {
        if (theme != null) {
            theme = theme.trim().toUpperCase(Locale.ROOT);
        }
    }
}
