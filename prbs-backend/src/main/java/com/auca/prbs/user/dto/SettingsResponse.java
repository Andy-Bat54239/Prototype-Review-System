package com.auca.prbs.user.dto;

import com.auca.prbs.user.entity.Settings;

/** All durations in minutes. */
public record SettingsResponse(
        int otpExpiry,
        int cancelWindow,
        int reminderTime
) {
    public static SettingsResponse of(Settings s) {
        return new SettingsResponse(s.getOtpExpiry(), s.getCancelWindow(), s.getReminderTime());
    }
}
