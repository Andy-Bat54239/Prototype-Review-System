package com.auca.prbs.booking.exception;

import com.auca.prbs.config.ApiException;
import org.springframework.http.HttpStatus;

public class InvalidBookingStatusException extends ApiException {
    public InvalidBookingStatusException(String detail) {
        super("INVALID_BOOKING_STATUS", HttpStatus.BAD_REQUEST, detail);
    }
}
