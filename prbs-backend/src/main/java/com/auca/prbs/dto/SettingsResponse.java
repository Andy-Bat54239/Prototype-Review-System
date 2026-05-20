package com.auca.prbs.dto;

import com.auca.prbs.entity.Settings;

/** All durations in minutes. Matches MOCK_SETTINGS_INIT in the React prototype. */
public record SettingsResponse(
        int otpExpiry,
        int cancelWindow,
        int reminderTime
) {
    public static SettingsResponse of(Settings s) {
        return new SettingsResponse(s.getOtpExpiry(), s.getCancelWindow(), s.getReminderTime());
    }
}
