package com.auca.prbs.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Student-side booking creation. Frontend picks an Availability row (which carries
 * supervisor, date, and meet URL) plus a specific slot time within that window.
 * Backend derives supervisorId / meetUrl / slotAt from the chosen Availability.
 */
public record CreateBookingRequest(
        @NotNull  Long   availabilityId,
        @NotBlank @Pattern(regexp = "\\d{2}:\\d{2}", message = "slotTime must be HH:MM") String slotTime,
        @NotBlank @Size(max = 200) String project,
        @Positive Integer groupNumber
) {}
