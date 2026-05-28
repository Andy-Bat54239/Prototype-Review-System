package com.auca.prbs.booking.dto;

import com.auca.prbs.booking.entity.BookingStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateBookingStatusRequest(@NotNull BookingStatus status) {}
