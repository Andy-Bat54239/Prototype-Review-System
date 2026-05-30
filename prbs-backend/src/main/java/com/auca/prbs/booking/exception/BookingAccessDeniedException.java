package com.auca.prbs.booking.exception;

import com.auca.prbs.config.ApiException;
import org.springframework.http.HttpStatus;

public class BookingAccessDeniedException extends ApiException {
    public BookingAccessDeniedException() {
        super("BOOKING_FORBIDDEN", HttpStatus.FORBIDDEN,
                "Only the booking's student or supervisor can perform this action");
    }
}
