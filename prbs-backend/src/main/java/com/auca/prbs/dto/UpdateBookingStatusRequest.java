package com.auca.prbs.dto;

import com.auca.prbs.entity.BookingStatus;
import jakarta.validation.constraints.NotNull;

/**
 * Supervisor marks a session as {@code COMPLETED} or {@code NO_SHOW} after the fact.
 * Other status transitions go through dedicated endpoints (cancel) or are invalid here.
 */
public record UpdateBookingStatusRequest(
        @NotNull BookingStatus status
) {}
