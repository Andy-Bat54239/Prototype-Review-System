package com.auca.prbs.exception;

import org.springframework.http.HttpStatus;

public class BookingNotFoundException extends ApiException {
    public BookingNotFoundException() {
        super("BOOKING_NOT_FOUND", HttpStatus.NOT_FOUND, "Booking not found");
    }
}
