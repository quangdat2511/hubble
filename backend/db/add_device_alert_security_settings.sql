ALTER TABLE user_settings
    ADD COLUMN IF NOT EXISTS new_device_login_alerts_enabled BOOLEAN DEFAULT TRUE;

ALTER TABLE user_sessions
    ADD COLUMN IF NOT EXISTS device_fingerprint VARCHAR(128);
