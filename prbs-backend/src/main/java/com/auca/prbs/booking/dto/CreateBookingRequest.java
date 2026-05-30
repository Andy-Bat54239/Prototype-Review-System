package com.auca.prbs.booking.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateBookingRequest(
        @NotNull  Long   availabilityId,
        @NotBlank @Pattern(regexp = "\\d{2}:\\d{2}", message = "slotTime must be HH:MM") String slotTime,
        @NotBlank @Size(max = 200) String project,
        @Positive Integer groupNumber
) {}
