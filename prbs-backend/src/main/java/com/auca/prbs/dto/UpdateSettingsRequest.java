package com.auca.prbs.dto;

import jakarta.validation.constraints.Positive;

/** All fields optional — only non-null ones are applied (partial update). */
public record UpdateSettingsRequest(
        @Positive Integer otpExpiry,
        @Positive Integer cancelWindow,
        @Positive Integer reminderTime
) {}
