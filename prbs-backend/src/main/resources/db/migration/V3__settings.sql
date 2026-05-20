CREATE TABLE settings (
    id              BIGINT     PRIMARY KEY,
    otp_expiry      INTEGER    NOT NULL,
    cancel_window   INTEGER    NOT NULL,
    reminder_time   INTEGER    NOT NULL,
    updated_at      TIMESTAMP  NOT NULL
);

-- Singleton row. otp_expiry / cancel_window / reminder_time in minutes,
-- matching MOCK_SETTINGS_INIT from the React prototype.
INSERT INTO settings (id, otp_expiry, cancel_window, reminder_time, updated_at)
VALUES (1, 10, 60, 30, CURRENT_TIMESTAMP);
