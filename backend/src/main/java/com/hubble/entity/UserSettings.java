package com.hubble.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
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
    UUID userId;

    @Column(name = "theme")
    String theme;

    @Column(name = "locale")
    String locale;

    @Column(name = "app_lock_pin")
    String appLockPin;

    @Column(name = "notification_enabled")
    Boolean notificationEnabled;

    @Column(name = "notification_sound")
    Boolean notificationSound;

    @Column(name = "updated_at")
    LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        applyDefaults();
        normalizeFields();
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        applyDefaults();
        normalizeFields();
        updatedAt = LocalDateTime.now();
    }

    private void applyDefaults() {
        if (theme == null) {
            theme = "DARK";
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
    }

    private void normalizeFields() {
        normalizeTheme();
        if (theme != null) {
            locale = locale.trim().toLowerCase(Locale.ROOT);
        }
    }

    public void normalizeTheme() {
        if (theme != null) {
            theme = theme.trim().toUpperCase(Locale.ROOT);
        }
    }
}
