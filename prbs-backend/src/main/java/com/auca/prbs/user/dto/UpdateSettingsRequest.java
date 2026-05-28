package com.auca.prbs.user.dto;

import jakarta.validation.constraints.Positive;

/** Partial update — only non-null fields are applied. */
public record UpdateSettingsRequest(
        @Positive Integer otpExpiry,
        @Positive Integer cancelWindow,
        @Positive Integer reminderTime
) {}
