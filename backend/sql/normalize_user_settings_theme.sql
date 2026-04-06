ALTER TABLE user_settings
    ALTER COLUMN theme SET DEFAULT 'DARK';

UPDATE user_settings
SET theme = UPPER(TRIM(theme))
WHERE theme IS NOT NULL
  AND theme <> UPPER(TRIM(theme));
