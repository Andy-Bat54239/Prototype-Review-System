package com.auca.prbs.availability.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;
import java.time.LocalTime;

public record AvailabilityRequest(
        @NotNull  LocalDate date,
        @NotNull  LocalTime startTime,
        @NotNull  LocalTime endTime,
        @Positive Integer   durationMinutes,
        @NotBlank String    meetUrl
) {}
