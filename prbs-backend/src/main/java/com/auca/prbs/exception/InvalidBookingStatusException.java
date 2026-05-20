package com.auca.prbs.exception;

import org.springframework.http.HttpStatus;

public class InvalidBookingStatusException extends ApiException {
    public InvalidBookingStatusException(String detail) {
        super("INVALID_BOOKING_STATUS", HttpStatus.BAD_REQUEST, detail);
    }
}
