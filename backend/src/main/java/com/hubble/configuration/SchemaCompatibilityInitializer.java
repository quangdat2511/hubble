package com.hubble.configuration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SchemaCompatibilityInitializer implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        applyAdditiveSchemaUpdates();
    }

    private void applyAdditiveSchemaUpdates() {
        ensureColumnExists(
                "ALTER TABLE IF EXISTS user_settings " +
                        "ADD COLUMN IF NOT EXISTS new_device_login_alerts_enabled BOOLEAN DEFAULT TRUE",
                "user_settings.new_device_login_alerts_enabled"
        );

        ensureColumnExists(
                "ALTER TABLE IF EXISTS user_sessions " +
                        "ADD COLUMN IF NOT EXISTS device_fingerprint VARCHAR(128)",
                "user_sessions.device_fingerprint"
        );
    }

    private void ensureColumnExists(String sql, String columnName) {
        try {
            jdbcTemplate.execute(sql);
            log.info("Ensured schema column exists: {}", columnName);
        } catch (Exception exception) {
            log.error("Failed to apply additive schema update for {}", columnName, exception);
            throw exception;
        }
    }
}
