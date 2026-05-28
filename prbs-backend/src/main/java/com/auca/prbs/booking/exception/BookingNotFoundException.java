package com.auca.prbs.booking.exception;

import com.auca.prbs.config.ApiException;
import org.springframework.http.HttpStatus;

public class BookingNotFoundException extends ApiException {
    public BookingNotFoundException() {
        super("BOOKING_NOT_FOUND", HttpStatus.NOT_FOUND, "Booking not found");
    }
}
