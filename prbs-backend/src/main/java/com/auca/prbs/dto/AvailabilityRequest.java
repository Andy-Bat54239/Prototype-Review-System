package com.auca.prbs.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;

public record AvailabilityRequest(
        @NotNull LocalDate date,
        @NotNull LocalTime startTime,
        @NotNull LocalTime endTime,
        @Positive int durationMinutes,
        @NotBlank @Size(max = 500) String meetUrl
) {
    @AssertTrue(message = "endTime must be after startTime")
    public boolean isEndAfterStart() {
        if (startTime == null || endTime == null) return true; // covered by @NotNull
        return endTime.isAfter(startTime);
    }
}
